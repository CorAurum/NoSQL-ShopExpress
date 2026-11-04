package uy.edu.utu.shopexpress.pedidos.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uy.edu.utu.shopexpress.pedidos.cadena.CadenaPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.dto.CrearPedidoRequest;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;
import uy.edu.utu.shopexpress.pedidos.model.EstadoPedido;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;
import uy.edu.utu.shopexpress.pedidos.model.ResultadoPaso;

import java.util.Optional;

/**
 * ORQUESTADOR CENTRAL de la SAGA (enfoque de orquestación).
 *
 * Responsabilidades:
 *  1. Idempotencia de la solicitud (Idempotency-Key).
 *  2. Ejecutar la secuencia de pasos (la Chain of Responsibility).
 *  3. Ante cualquier fallo, ejecutar las compensaciones en orden INVERSO (LIFO) y
 *     dejar el pedido en CANCELADO con el motivo.
 *  4. Registrar cada paso y cada compensación en la bitácora del pedido (pasos_saga).
 *
 * Los demás servicios no conocen la SAGA: solo exponen operaciones y sus
 * compensaciones; la lógica de coordinación está en un único lugar.
 */
@Service
public class OrquestadorSagaPedidos {

    private static final Logger log = LoggerFactory.getLogger(OrquestadorSagaPedidos.class);

    private final CadenaPedido cadena;
    private final RegistroPedidos registro;
    private final ControlIdempotencia idempotencia;

    public OrquestadorSagaPedidos(CadenaPedido cadena, RegistroPedidos registro, ControlIdempotencia idempotencia) {
        this.cadena = cadena;
        this.registro = registro;
        this.idempotencia = idempotencia;
    }

    public ResultadoPedido crearPedido(CrearPedidoRequest solicitud, String idempotencyKey, String simularFallo) {
        String clave = (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey.trim();

        if (clave != null) {
            Optional<Pedido> previo = idempotencia.buscarPedidoPrevio(clave);
            if (previo.isPresent()) {
                return ResultadoPedido.repetido(previo.get());
            }
            if (!idempotencia.adquirir(clave)) {
                throw new NegocioException(HttpStatus.CONFLICT, "SOLICITUD_EN_PROCESO",
                        "Ya se está procesando un pedido con esta Idempotency-Key");
            }
        }

        ContextoPedido ctx = new ContextoPedido(solicitud, clave, simularFallo);
        try {
            cadena.procesar(ctx);
            Pedido pedido = ctx.getPedido();
            if (pedido != null) {
                // Guarda la bitácora completa: los últimos pasos (confirmación e historial)
                // se registran después del último guardado que hace cada manejador.
                if (ctx.isPedidoPersistido()) {
                    guardarSinFallar(ctx);
                }
                idempotencia.completar(clave, pedido.getId());
                log.info("Pedido {} CONFIRMADO", pedido.getId());
            }
            return ResultadoPedido.exitoso(pedido);
        } catch (NegocioException e) {
            return fallar(ctx, e);
        } catch (RuntimeException e) {
            log.error("Error inesperado en el paso {}", ctx.getPasoEnCurso(), e);
            return fallar(ctx, new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                    "Error inesperado en " + ctx.getPasoEnCurso() + ": " + e.getMessage()));
        }
    }

    private ResultadoPedido fallar(ContextoPedido ctx, NegocioException causa) {
        log.warn("SAGA fallida en {}: {} - {}", ctx.getPasoEnCurso(), causa.getCodigo(), causa.getMessage());
        ctx.registrarPaso(ctx.getPasoEnCurso(), ResultadoPaso.FALLO, causa.getCodigo() + ": " + causa.getMessage());

        if (ctx.isPedidoPersistido()) {
            ctx.getPedido().setEstado(EstadoPedido.COMPENSANDO);
            guardarSinFallar(ctx);
        }

        compensar(ctx);

        if (ctx.isPedidoPersistido()) {
            Pedido pedido = ctx.getPedido();
            pedido.setEstado(EstadoPedido.CANCELADO);
            pedido.setMotivoFallo(causa.getCodigo() + ": " + causa.getMessage());
            guardarSinFallar(ctx);
            // La clave queda asociada al pedido cancelado: repetir la misma solicitud
            // devuelve el mismo resultado en vez de volver a cobrar o reservar.
            idempotencia.completar(ctx.getIdempotencyKey(), pedido.getId());
            return ResultadoPedido.fallido(pedido, causa);
        }

        // Falló en una validación previa: no se creó nada, se puede reintentar.
        idempotencia.liberar(ctx.getIdempotencyKey());
        return ResultadoPedido.fallido(null, causa);
    }

    /** Ejecuta las compensaciones en orden inverso. Un error no detiene las siguientes. */
    private void compensar(ContextoPedido ctx) {
        while (!ctx.getCompensables().isEmpty()) {
            ManejadorPedido paso = ctx.getCompensables().pop();
            String nombre = "COMPENSAR_" + paso.nombre();
            try {
                String detalle = paso.compensar(ctx);
                ctx.registrarPaso(nombre, ResultadoPaso.COMPENSADO, detalle);
            } catch (RuntimeException e) {
                // Queda registrado para reintento manual o automático (las compensaciones son idempotentes).
                log.error("Falló la compensación {} del pedido {}", nombre,
                        ctx.getPedido() == null ? "-" : ctx.getPedido().getId(), e);
                ctx.registrarPaso(nombre, ResultadoPaso.ERROR_COMPENSACION, e.getMessage());
            }
        }
    }

    private void guardarSinFallar(ContextoPedido ctx) {
        try {
            registro.guardar(ctx);
        } catch (RuntimeException e) {
            log.error("No se pudo guardar el estado del pedido {}", ctx.getPedido().getId(), e);
        }
    }
}
