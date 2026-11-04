package uy.edu.utu.shopexpress.pedidos.dto;

import uy.edu.utu.shopexpress.pedidos.model.Pedido;

import java.time.Instant;
import java.util.List;

/** Respuesta de error de POST /api/pedidos. Incluye el pedido si llegó a persistirse (ya CANCELADO). */
public record ErrorPedido(String error, String mensaje, List<String> detalles, Pedido pedido, Instant timestamp) {
}
