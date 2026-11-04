package uy.edu.utu.shopexpress.pedidos.model;

/** Pago embebido en el pedido (como en el anexo de la letra). */
public record InfoPago(String metodo, String estadoPago, String transaccionId, String motivoRechazo) {
}
