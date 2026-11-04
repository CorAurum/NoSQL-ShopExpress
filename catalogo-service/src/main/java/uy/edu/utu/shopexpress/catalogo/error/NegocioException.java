package uy.edu.utu.shopexpress.catalogo.error;

import org.springframework.http.HttpStatus;

public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public NegocioException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }

    public String getCodigo() { return codigo; }
}
