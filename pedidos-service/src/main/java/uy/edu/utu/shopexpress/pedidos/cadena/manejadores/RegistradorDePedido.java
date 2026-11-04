package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;
import uy.edu.utu.shopexpress.pedidos.model.EstadoPedido;
import uy.edu.utu.shopexpress.pedidos.model.InfoPago;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;
import uy.edu.utu.shopexpress.pedidos.saga.RegistroPedidos;
import uy.edu.utu.shopexpress.pedidos.util.Ids;

import java.time.Instant;

/**
 * Eslabón 5 / Paso SAGA T1: crea el pedido en estado PENDIENTE con un ID único.
 * Compensación C1: el pedido pasa a CANCELADO (nunca se borra: queda el rastro).
 */
@Component
public class RegistradorDePedido extends ManejadorPedido {

    private final RegistroPedidos registro;

    public RegistradorDePedido(RegistroPedidos registro) {
        this.registro = registro;
    }

    @Override
    protected void manejar(ContextoPedido ctx) {
        Instant ahora = Instant.now();
        Pedido pedido = new Pedido();
        pedido.setId(Ids.nuevo("ord"));
        pedido.setIdempotencyKey(ctx.getIdempotencyKey());
        pedido.setUsuarioId(ctx.getUsuario().id());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setItems(ctx.getItems());
        pedido.setTotal(ctx.getTotal());
        pedido.setPago(new InfoPago(ctx.getSolicitud().pago().metodo(), "PENDIENTE", null, null));
        pedido.setFechaCreacion(ahora);
        ctx.setPedido(pedido);
        try {
            registro.guardar(ctx);
        } catch (DuplicateKeyException e) {
            throw new NegocioException(HttpStatus.CONFLICT, "PEDIDO_DUPLICADO",
                    "Ya existe un pedido para esta Idempotency-Key");
        }
    }

    @Override
    protected boolean esCompensable() {
        return true;
    }

    @Override
    public String compensar(ContextoPedido ctx) {
        if (!ctx.isPedidoPersistido()) {
            return "El pedido no llegó a crearse";
        }
        ctx.getPedido().setEstado(EstadoPedido.CANCELADO);
        return "Pedido marcado como CANCELADO";
    }

    @Override
    public String nombre() {
        return "CREAR_PEDIDO";
    }
}
