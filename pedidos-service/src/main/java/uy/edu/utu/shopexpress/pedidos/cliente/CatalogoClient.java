package uy.edu.utu.shopexpress.pedidos.cliente;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CatalogoClient {

    public record ProductoRemoto(String id, String nombre, BigDecimal precio, int stockDisponible) {
    }

    public record ItemReserva(String productoId, int cantidad) {
    }

    public record ReservaRemota(String pedidoId, String estado, String motivo) {
    }

    public record SolicitudReserva(String pedidoId, List<ItemReserva> items) {
    }

    private final RestClient http;
    private final ObjectMapper mapper;

    public CatalogoClient(RestClient.Builder builder, ObjectMapper mapper,
                          @Value("${servicios.catalogo.url}") String url,
                          @Value("${servicios.http.timeout-conexion-ms}") int timeoutConexion,
                          @Value("${servicios.http.timeout-lectura-ms}") int timeoutLectura) {
        this.http = ClienteHttp.crear(builder, url, timeoutConexion, timeoutLectura);
        this.mapper = mapper;
    }

    public ProductoRemoto obtenerProducto(String productoId) {
        try {
            return http.get().uri("/api/productos/{id}", productoId)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new NegocioException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO",
                                "No existe el producto " + productoId);
                    })
                    .body(ProductoRemoto.class);
        } catch (RestClientException e) {
            throw ClienteHttp.noDisponible("catálogo", e);
        }
    }

    public ReservaRemota reservar(String pedidoId, List<ItemReserva> items) {
        try {
            return http.post().uri("/api/inventario/reservas")
                    .body(new SolicitudReserva(pedidoId, items))
                    .retrieve()
                    .onStatus(s -> s.value() == 409, (req, res) -> {
                        throw new NegocioException(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE",
                                ClienteHttp.mensajeRemoto(mapper, res));
                    })
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new NegocioException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO",
                                ClienteHttp.mensajeRemoto(mapper, res));
                    })
                    .body(ReservaRemota.class);
        } catch (RestClientException e) {
            throw ClienteHttp.noDisponible("catálogo", e);
        }
    }

    public ReservaRemota confirmar(String pedidoId) {
        return accionSobreReserva(pedidoId, "confirmar");
    }

    public ReservaRemota liberar(String pedidoId) {
        return accionSobreReserva(pedidoId, "liberar");
    }

    private ReservaRemota accionSobreReserva(String pedidoId, String accion) {
        try {
            return http.post().uri("/api/inventario/reservas/{id}/" + accion, pedidoId)
                    .retrieve()
                    .onStatus(s -> s.is4xxClientError(), (req, res) -> {
                        throw new NegocioException(HttpStatus.CONFLICT, "RESERVA_" + accion.toUpperCase(),
                                "No se pudo " + accion + " la reserva: " + ClienteHttp.mensajeRemoto(mapper, res));
                    })
                    .body(ReservaRemota.class);
        } catch (RestClientException e) {
            throw ClienteHttp.noDisponible("catálogo", e);
        }
    }
}
