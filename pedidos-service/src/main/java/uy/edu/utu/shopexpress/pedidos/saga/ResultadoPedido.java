package uy.edu.utu.shopexpress.pedidos.saga;

import uy.edu.utu.shopexpress.pedidos.error.NegocioException;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;

/**
 * Resultado de la SAGA.
 *  - pedido: el pedido persistido (null si falló antes de crearse).
 *  - error: causa del fallo (null si fue exitoso).
 *  - repetido: true si se devolvió un pedido previo por la Idempotency-Key.
 */
public record ResultadoPedido(Pedido pedido, NegocioException error, boolean repetido) {

    public static ResultadoPedido exitoso(Pedido pedido) {
        return new ResultadoPedido(pedido, null, false);
    }

    public static ResultadoPedido repetido(Pedido pedido) {
        return new ResultadoPedido(pedido, null, true);
    }

    public static ResultadoPedido fallido(Pedido pedido, NegocioException error) {
        return new ResultadoPedido(pedido, error, false);
    }
}
