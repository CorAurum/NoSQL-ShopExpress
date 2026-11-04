package uy.edu.utu.shopexpress.catalogo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReponerStockRequest(@NotNull @Min(1) Integer cantidad) {
}
