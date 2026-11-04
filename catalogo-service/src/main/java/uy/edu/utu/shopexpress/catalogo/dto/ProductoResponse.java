package uy.edu.utu.shopexpress.catalogo.dto;

import uy.edu.utu.shopexpress.catalogo.model.Categoria;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ProductoResponse(
        String id,
        String sku,
        String nombre,
        String descripcion,
        Categoria categoria,
        BigDecimal precio,
        int stockDisponible,
        int stockReservado,
        Map<String, String> atributos,
        Instant fechaActualizacion) {
}
