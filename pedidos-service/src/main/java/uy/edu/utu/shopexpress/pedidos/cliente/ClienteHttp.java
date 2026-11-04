package uy.edu.utu.shopexpress.pedidos.cliente;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Utilidades comunes a los clientes HTTP hacia otros microservicios. */
final class ClienteHttp {

    private ClienteHttp() {
    }

    /**
     * RestClient con timeouts: una dependencia lenta no debe colgar la SAGA indefinidamente.
     *
     * Se usa el HttpClient del JDK (HTTP/1.1) y el cuerpo se envía con Content-Length
     * conocido (BufferingClientHttpRequestFactory). Con HttpURLConnection y envío por
     * chunks, en Docker sobre WSL2 el cuerpo de algunos POST llegaba tarde al otro
     * servicio: la llamada vencía por timeout y la operación se aplicaba después.
     */
    static RestClient crear(RestClient.Builder builder, String urlBase, int timeoutConexionMs, int timeoutLecturaMs) {
        HttpClient cliente = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(timeoutConexionMs))
                .build();
        JdkClientHttpRequestFactory jdk = new JdkClientHttpRequestFactory(cliente);
        jdk.setReadTimeout(Duration.ofMillis(timeoutLecturaMs));
        return builder.clone()
                .baseUrl(urlBase)
                .requestFactory(new BufferingClientHttpRequestFactory(jdk))
                .build();
    }

    /** Extrae el campo "mensaje" del cuerpo de error del servicio remoto, si existe. */
    static String mensajeRemoto(ObjectMapper mapper, ClientHttpResponse respuesta) {
        try {
            String cuerpo = new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8);
            String mensaje = mapper.readTree(cuerpo).path("mensaje").asText("");
            return mensaje.isBlank() ? cuerpo : mensaje;
        } catch (IOException | RuntimeException e) {
            return "sin detalle";
        }
    }

    static NegocioException noDisponible(String servicio, Exception causa) {
        return new NegocioException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE",
                "El servicio de " + servicio + " no está disponible: " + causa.getMessage());
    }
}
