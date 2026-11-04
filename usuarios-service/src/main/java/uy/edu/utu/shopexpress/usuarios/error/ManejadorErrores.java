package uy.edu.utu.shopexpress.usuarios.error;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorApi> negocio(NegocioException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorApi(e.getCodigo(), e.getMessage(), null, Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorApi("DATOS_INVALIDOS", "La solicitud no es válida", detalles, Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> jsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorApi("JSON_INVALIDO", "El cuerpo de la solicitud no es un JSON válido", null, Instant.now()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorApi> duplicado(DuplicateKeyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorApi("USUARIO_DUPLICADO", "Ya existe un usuario con ese email o documento", null, Instant.now()));
    }
}
