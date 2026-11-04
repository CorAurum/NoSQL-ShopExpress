package uy.edu.utu.shopexpress.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

public record DireccionRequest(
        @NotBlank String calle,
        @NotBlank String ciudad,
        @NotBlank String codigoPostal) {
}
