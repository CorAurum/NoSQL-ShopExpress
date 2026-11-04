package uy.edu.utu.shopexpress.pedidos.model;

import java.math.BigDecimal;

/**
 * Ítem embebido en el pedido. producto_id es una REFERENCIA al catálogo, pero nombre y
 * precio_unitario se COPIAN: son una foto del momento de la compra y no deben cambiar
 * si mañana el catálogo actualiza el precio.
 */
public record ItemPedido(String productoId, String nombre, int cantidad, BigDecimal precioUnitario,
                         BigDecimal subtotal) {
}
