package uy.edu.utu.shopexpress.catalogo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record CrearProductoRequest(
        @NotBlank String sku,
        @NotBlank String nombre,
        String descripcion,
        @NotNull @Valid CategoriaRequest categoria,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precio,
        @NotNull @Min(0) Integer stockInicial,
        Map<String, String> atributos) {
}
