package uy.edu.utu.shopexpress.usuarios.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CrearUsuarioRequest(
        @NotBlank String tipoDocumento,
        @NotBlank @Size(max = 20) String nroDocumento,
        @NotBlank String nombre,
        @NotBlank String apellido,
        @NotBlank @Email String email,
        @NotEmpty List<@Valid DireccionRequest> direcciones) {
}
