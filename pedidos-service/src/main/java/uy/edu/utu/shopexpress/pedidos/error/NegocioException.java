package uy.edu.utu.shopexpress.pedidos.error;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Error de negocio con su código HTTP. Cualquier manejador de la cadena lanza esta
 * excepción para cortar el flujo; el orquestador la captura y dispara las compensaciones.
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final List<String> detalles;

    public NegocioException(HttpStatus status, String codigo, String mensaje) {
        this(status, codigo, mensaje, null);
    }

    public NegocioException(HttpStatus status, String codigo, String mensaje, List<String> detalles) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
        this.detalles = detalles;
    }

    public HttpStatus getStatus() { return status; }

    public String getCodigo() { return codigo; }

    public List<String> getDetalles() { return detalles; }
}
