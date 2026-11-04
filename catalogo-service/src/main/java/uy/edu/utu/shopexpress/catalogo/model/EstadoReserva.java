package uy.edu.utu.shopexpress.catalogo.model;

/**
 * EN_PROCESO  -> se están descontando unidades.
 * RESERVADA   -> unidades apartadas (stock_disponible bajó, stock_reservado subió).
 * CONFIRMADA  -> venta cerrada: las unidades salen del stock reservado.
 * LIBERADA    -> compensación: las unidades volvieron a stock_disponible.
 * RECHAZADA   -> no había stock suficiente; no quedó nada descontado.
 */
public enum EstadoReserva {
    EN_PROCESO,
    RESERVADA,
    CONFIRMADA,
    LIBERADA,
    RECHAZADA
}
