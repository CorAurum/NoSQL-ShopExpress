package uy.edu.utu.shopexpress.pedidos.error;

import java.time.Instant;
import java.util.List;

public record ErrorApi(String error, String mensaje, List<String> detalles, Instant timestamp) {
}
