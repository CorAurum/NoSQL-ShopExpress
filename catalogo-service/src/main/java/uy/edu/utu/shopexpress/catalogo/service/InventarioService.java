package uy.edu.utu.shopexpress.catalogo.service;

import com.mongodb.client.result.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uy.edu.utu.shopexpress.catalogo.cache.CacheProductos;
import uy.edu.utu.shopexpress.catalogo.dto.ReservaRequest;
import uy.edu.utu.shopexpress.catalogo.dto.ReservaResponse;
import uy.edu.utu.shopexpress.catalogo.error.NegocioException;
import uy.edu.utu.shopexpress.catalogo.model.EstadoReserva;
import uy.edu.utu.shopexpress.catalogo.model.ItemReserva;
import uy.edu.utu.shopexpress.catalogo.model.Producto;
import uy.edu.utu.shopexpress.catalogo.model.Reserva;
import uy.edu.utu.shopexpress.catalogo.repository.ProductoRepository;
import uy.edu.utu.shopexpress.catalogo.repository.ReservaRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Lado CP del sistema: las operaciones de stock priorizan CONSISTENCIA.
 *
 * Garantía anti-sobreventa: cada unidad se descuenta con una actualización
 * CONDICIONAL y ATÓMICA sobre un único documento:
 *
 *     updateOne({_id: prod, stock_disponible: {$gte: n}},
 *               {$inc: {stock_disponible: -n, stock_reservado: +n}})
 *
 * MongoDB evalúa el filtro y aplica el $inc como una sola operación sobre el
 * documento, así que dos pedidos concurrentes nunca pueden llevar el stock por debajo
 * de cero: el segundo simplemente no encuentra un documento que cumpla el filtro.
 *
 * Todas las operaciones son IDEMPOTENTES respecto al pedido_id, porque el orquestador
 * SAGA puede reintentarlas (por ejemplo, tras un timeout).
 */
@Service
public class InventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    private final MongoTemplate mongo;
    private final ReservaRepository reservas;
    private final ProductoRepository productos;
    private final CacheProductos cache;

    public InventarioService(MongoTemplate mongo, ReservaRepository reservas,
                             ProductoRepository productos, CacheProductos cache) {
        this.mongo = mongo;
        this.reservas = reservas;
        this.productos = productos;
        this.cache = cache;
    }

    /** Paso SAGA "reservar stock": todo o nada para el conjunto de ítems del pedido. */
    public ReservaResponse reservar(ReservaRequest solicitud) {
        String pedidoId = solicitud.pedidoId();
        List<ItemReserva> items = agrupar(solicitud.items());

        // 1) La reserva se registra primero. Su _id es el pedido_id, por lo que actúa
        //    como "candado": un reintento o un duplicado concurrente choca con la clave.
        try {
            reservas.insert(new Reserva(pedidoId, items, EstadoReserva.EN_PROCESO));
        } catch (DuplicateKeyException e) {
            return reservaExistente(pedidoId);
        }

        // 2) Descuento atómico ítem por ítem. Si alguno no alcanza, se revierten los ya
        //    aplicados y la reserva queda RECHAZADA.
        List<ItemReserva> aplicados = new ArrayList<>();
        try {
            for (ItemReserva item : items) {
                UpdateResult r = mongo.updateFirst(
                        query(where("id").is(item.productoId()).and("stockDisponible").gte(item.cantidad())),
                        new Update().inc("stockDisponible", -item.cantidad())
                                .inc("stockReservado", item.cantidad())
                                .set("fechaActualizacion", Instant.now()),
                        Producto.class);
                if (r.getModifiedCount() == 0) {
                    NegocioException error = describirFalta(item);
                    revertir(aplicados);
                    cerrar(pedidoId, EstadoReserva.RECHAZADA, error.getMessage());
                    throw error;
                }
                aplicados.add(item);
            }
        } catch (NegocioException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Error inesperado reservando stock del pedido {}", pedidoId, e);
            revertir(aplicados);
            cerrar(pedidoId, EstadoReserva.RECHAZADA, "Error interno: " + e.getMessage());
            throw e;
        } finally {
            cache.invalidar(items.stream().map(ItemReserva::productoId).toList());
        }

        // 3) EN_PROCESO -> RESERVADA de forma atómica. Si mientras tanto el orquestador
        //    pidió liberar (porque su llamada venció por timeout), se libera enseguida:
        //    así no quedan unidades reservadas para un pedido que ya se canceló.
        Reserva reservada = mongo.findAndModify(
                query(where("id").is(pedidoId).and("estado").is(EstadoReserva.EN_PROCESO)),
                new Update().set("estado", EstadoReserva.RESERVADA).set("fechaActualizacion", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                Reserva.class);
        if (reservada != null && reservada.isLiberacionPendiente()) {
            log.warn("La reserva {} terminó después de un pedido de liberación; se libera ahora", pedidoId);
            return liberar(pedidoId);
        }
        return aRespuesta(reservas.findById(pedidoId).orElseThrow());
    }

    /** Compensación SAGA: devuelve las unidades. Solo actúa si la reserva estaba RESERVADA. */
    public ReservaResponse liberar(String pedidoId) {
        Reserva liberada = mongo.findAndModify(
                query(where("id").is(pedidoId).and("estado").is(EstadoReserva.RESERVADA)),
                new Update().set("estado", EstadoReserva.LIBERADA).set("fechaActualizacion", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                Reserva.class);

        if (liberada != null) {
            for (ItemReserva item : liberada.getItems()) {
                mongo.updateFirst(query(where("id").is(item.productoId())),
                        new Update().inc("stockDisponible", item.cantidad())
                                .inc("stockReservado", -item.cantidad())
                                .set("fechaActualizacion", Instant.now()),
                        Producto.class);
            }
            cache.invalidar(liberada.getItems().stream().map(ItemReserva::productoId).toList());
            return aRespuesta(liberada);
        }

        Optional<Reserva> existente = reservas.findById(pedidoId);
        if (existente.isEmpty()) {
            // Nunca llegó a reservarse (p. ej. el pedido falló antes): no hay nada que liberar.
            return new ReservaResponse(pedidoId, "INEXISTENTE", List.of(), "No había reserva para liberar", Instant.now());
        }
        return switch (existente.get().getEstado()) {
            case LIBERADA, RECHAZADA -> aRespuesta(existente.get()); // reintento: ya estaba compensada
            case CONFIRMADA -> throw new NegocioException(HttpStatus.CONFLICT, "RESERVA_CONFIRMADA",
                    "La reserva del pedido " + pedidoId + " ya fue confirmada y no puede liberarse");
            case EN_PROCESO -> marcarLiberacionPendiente(pedidoId);
            case RESERVADA -> liberar(pedidoId); // cambió de estado entre las dos lecturas: reintentar
        };
    }

    /** La reserva aún se está aplicando: se deja anotado que debe liberarse al terminar. */
    private ReservaResponse marcarLiberacionPendiente(String pedidoId) {
        UpdateResult r = mongo.updateFirst(
                query(where("id").is(pedidoId).and("estado").is(EstadoReserva.EN_PROCESO)),
                new Update().set("liberacionPendiente", true).set("fechaActualizacion", Instant.now()),
                Reserva.class);
        if (r.getModifiedCount() == 0) {
            return liberar(pedidoId); // terminó justo ahora: reintentar con el estado nuevo
        }
        return new ReservaResponse(pedidoId, "LIBERACION_PENDIENTE", List.of(),
                "Se liberará en cuanto termine de aplicarse", Instant.now());
    }

    /** Paso SAGA "confirmar": la venta se cierra y las unidades salen del stock reservado. */
    public ReservaResponse confirmar(String pedidoId) {
        Reserva confirmada = mongo.findAndModify(
                query(where("id").is(pedidoId).and("estado").is(EstadoReserva.RESERVADA)),
                new Update().set("estado", EstadoReserva.CONFIRMADA).set("fechaActualizacion", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                Reserva.class);

        if (confirmada != null) {
            for (ItemReserva item : confirmada.getItems()) {
                mongo.updateFirst(query(where("id").is(item.productoId())),
                        new Update().inc("stockReservado", -item.cantidad()).set("fechaActualizacion", Instant.now()),
                        Producto.class);
            }
            cache.invalidar(confirmada.getItems().stream().map(ItemReserva::productoId).toList());
            return aRespuesta(confirmada);
        }

        Reserva existente = reservas.findById(pedidoId).orElseThrow(() -> new NegocioException(
                HttpStatus.NOT_FOUND, "RESERVA_NO_ENCONTRADA", "No existe reserva para el pedido " + pedidoId));
        if (existente.getEstado() == EstadoReserva.CONFIRMADA) {
            return aRespuesta(existente); // reintento idempotente
        }
        throw new NegocioException(HttpStatus.CONFLICT, "RESERVA_NO_CONFIRMABLE",
                "La reserva del pedido " + pedidoId + " está " + existente.getEstado() + " y no puede confirmarse");
    }

    public ReservaResponse obtener(String pedidoId) {
        return reservas.findById(pedidoId).map(InventarioService::aRespuesta)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "RESERVA_NO_ENCONTRADA",
                        "No existe reserva para el pedido " + pedidoId));
    }

    // ---------------------------------------------------------------- auxiliares

    private ReservaResponse reservaExistente(String pedidoId) {
        Reserva existente = reservas.findById(pedidoId).orElseThrow();
        return switch (existente.getEstado()) {
            case RESERVADA, CONFIRMADA -> aRespuesta(existente);
            case EN_PROCESO -> throw new NegocioException(HttpStatus.CONFLICT, "RESERVA_EN_PROCESO",
                    "Ya hay una reserva en proceso para el pedido " + pedidoId);
            case RECHAZADA -> throw new NegocioException(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE",
                    existente.getMotivo());
            case LIBERADA -> throw new NegocioException(HttpStatus.CONFLICT, "RESERVA_LIBERADA",
                    "La reserva del pedido " + pedidoId + " ya fue liberada");
        };
    }

    /** Une líneas repetidas del mismo producto y ordena por producto_id (orden determinístico). */
    private static List<ItemReserva> agrupar(List<ReservaRequest.Item> items) {
        TreeMap<String, Integer> porProducto = new TreeMap<>();
        items.forEach(i -> porProducto.merge(i.productoId(), i.cantidad(), Integer::sum));
        return porProducto.entrySet().stream().map(e -> new ItemReserva(e.getKey(), e.getValue())).toList();
    }

    private NegocioException describirFalta(ItemReserva item) {
        return productos.findById(item.productoId())
                .map(p -> new NegocioException(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE",
                        "Stock insuficiente para " + p.getNombre() + " (" + p.getId() + "): solicitado "
                                + item.cantidad() + ", disponible " + p.getStockDisponible()))
                .orElseGet(() -> ProductoService.noEncontrado(item.productoId()));
    }

    private void revertir(List<ItemReserva> aplicados) {
        for (ItemReserva item : aplicados) {
            mongo.updateFirst(query(where("id").is(item.productoId())),
                    new Update().inc("stockDisponible", item.cantidad()).inc("stockReservado", -item.cantidad()),
                    Producto.class);
        }
    }

    private void cerrar(String pedidoId, EstadoReserva estado, String motivo) {
        Update update = new Update().set("estado", estado).set("fechaActualizacion", Instant.now());
        if (motivo != null) {
            update.set("motivo", motivo);
        }
        mongo.updateFirst(query(where("id").is(pedidoId)), update, Reserva.class);
    }

    private static ReservaResponse aRespuesta(Reserva r) {
        return new ReservaResponse(r.getId(), r.getEstado().name(), r.getItems(), r.getMotivo(),
                r.getFechaActualizacion());
    }
}
