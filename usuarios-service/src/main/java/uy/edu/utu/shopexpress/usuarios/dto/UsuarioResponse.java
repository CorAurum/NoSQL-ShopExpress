package uy.edu.utu.shopexpress.usuarios.dto;

import uy.edu.utu.shopexpress.usuarios.model.Direccion;
import uy.edu.utu.shopexpress.usuarios.model.ResumenCompra;

import java.time.Instant;
import java.util.List;

/**
 * Vista pública del usuario. El número de documento se devuelve ENMASCARADO
 * (minimización de datos): así tampoco queda en claro dentro de la caché de Redis.
 */
public record UsuarioResponse(
        String id,
        String tipoDocumento,
        String nroDocumento,
        String nombre,
        String apellido,
        String email,
        List<Direccion> direcciones,
        List<ResumenCompra> historialCompras,
        Instant fechaAlta) {
}
