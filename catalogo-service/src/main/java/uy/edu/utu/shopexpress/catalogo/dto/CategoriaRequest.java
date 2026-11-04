package uy.edu.utu.shopexpress.catalogo.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequest(@NotBlank String codigo, @NotBlank String nombre) {
}
