package uy.edu.utu.shopexpress.catalogo.dto;

import uy.edu.utu.shopexpress.catalogo.model.ItemReserva;

import java.time.Instant;
import java.util.List;

public record ReservaResponse(String pedidoId, String estado, List<ItemReserva> items, String motivo,
                              Instant fechaActualizacion) {
}
