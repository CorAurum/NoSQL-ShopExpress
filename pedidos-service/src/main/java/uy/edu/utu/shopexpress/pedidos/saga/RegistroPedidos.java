package uy.edu.utu.shopexpress.pedidos.saga;

import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;
import uy.edu.utu.shopexpress.pedidos.repository.PedidoRepository;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Persiste el estado del pedido en cada transición de la SAGA. Guardar el estado
 * paso a paso permite saber dónde quedó una SAGA si el proceso se cae a mitad.
 */
@Component
public class RegistroPedidos {

    private final PedidoRepository repositorio;

    public RegistroPedidos(PedidoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void guardar(ContextoPedido ctx) {
        Pedido pedido = ctx.getPedido();
        pedido.setPasosSaga(new ArrayList<>(ctx.getPasos()));
        pedido.setFechaActualizacion(Instant.now());
        if (ctx.isPedidoPersistido()) {
            repositorio.save(pedido);
        } else {
            repositorio.insert(pedido);
            ctx.marcarPersistido();
        }
    }
}
