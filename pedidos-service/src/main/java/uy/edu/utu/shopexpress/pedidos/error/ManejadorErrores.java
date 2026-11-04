package uy.edu.utu.shopexpress.pedidos.error;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorApi> negocio(NegocioException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorApi(e.getCodigo(), e.getMessage(), e.getDetalles(), Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> jsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorApi("JSON_INVALIDO", "El cuerpo de la solicitud no es un JSON válido", null, Instant.now()));
    }
}
