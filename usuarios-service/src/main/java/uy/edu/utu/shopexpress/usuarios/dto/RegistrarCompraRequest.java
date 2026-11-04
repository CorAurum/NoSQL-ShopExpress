package uy.edu.utu.shopexpress.usuarios.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Payload interno que envía el Servicio de Pedidos al confirmar una compra. */
public record RegistrarCompraRequest(
        @NotBlank String pedidoId,
        @NotNull BigDecimal total,
        @Min(1) int cantidadItems) {
}
