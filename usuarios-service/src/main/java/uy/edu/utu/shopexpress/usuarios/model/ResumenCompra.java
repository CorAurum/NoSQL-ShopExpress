package uy.edu.utu.shopexpress.usuarios.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Referencia extendida a un pedido: el pedido_id apunta al Servicio de Pedidos y se
 * copian solo los datos necesarios para mostrar el historial sin otra llamada.
 */
public record ResumenCompra(String pedidoId, Instant fecha, BigDecimal total, int cantidadItems) {
}
