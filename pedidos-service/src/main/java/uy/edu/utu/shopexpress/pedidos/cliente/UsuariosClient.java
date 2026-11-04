package uy.edu.utu.shopexpress.pedidos.cliente;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;

import java.math.BigDecimal;

@Component
public class UsuariosClient {

    public record UsuarioRemoto(String id, String nombre, String apellido, String email) {
    }

    public record RegistrarCompra(String pedidoId, BigDecimal total, int cantidadItems) {
    }

    private final RestClient http;

    public UsuariosClient(RestClient.Builder builder,
                          @Value("${servicios.usuarios.url}") String url,
                          @Value("${servicios.http.timeout-conexion-ms}") int timeoutConexion,
                          @Value("${servicios.http.timeout-lectura-ms}") int timeoutLectura) {
        this.http = ClienteHttp.crear(builder, url, timeoutConexion, timeoutLectura);
    }

    public UsuarioRemoto obtener(String usuarioId) {
        try {
            return http.get().uri("/api/usuarios/{id}", usuarioId)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> {
                        throw new NegocioException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO",
                                "No existe el usuario " + usuarioId);
                    })
                    .body(UsuarioRemoto.class);
        } catch (RestClientException e) {
            throw ClienteHttp.noDisponible("usuarios", e);
        }
    }

    public void registrarCompra(String usuarioId, String pedidoId, BigDecimal total, int cantidadItems) {
        try {
            http.post().uri("/api/usuarios/{id}/compras", usuarioId)
                    .body(new RegistrarCompra(pedidoId, total, cantidadItems))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw ClienteHttp.noDisponible("usuarios", e);
        }
    }
}
