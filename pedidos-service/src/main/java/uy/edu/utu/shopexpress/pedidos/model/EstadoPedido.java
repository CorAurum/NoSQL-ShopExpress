package uy.edu.utu.shopexpress.pedidos.model;

/**
 * Máquina de estados del pedido durante la SAGA.
 *
 *  PENDIENTE -> STOCK_RESERVADO -> PAGADO -> CONFIRMADO     (camino feliz)
 *  cualquier estado intermedio -> COMPENSANDO -> CANCELADO   (fallo)
 */
public enum EstadoPedido {
    PENDIENTE,
    STOCK_RESERVADO,
    PAGADO,
    CONFIRMADO,
    COMPENSANDO,
    CANCELADO
}
