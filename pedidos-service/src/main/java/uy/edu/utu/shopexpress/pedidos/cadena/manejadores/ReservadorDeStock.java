package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.cliente.CatalogoClient;
import uy.edu.utu.shopexpress.pedidos.cliente.CatalogoClient.ItemReserva;
import uy.edu.utu.shopexpress.pedidos.cliente.CatalogoClient.ReservaRemota;
import uy.edu.utu.shopexpress.pedidos.model.EstadoPedido;
import uy.edu.utu.shopexpress.pedidos.saga.RegistroPedidos;

/**
 * Eslabón 6 / Paso SAGA T2: reserva el stock en el Servicio de Catálogo (operación
 * atómica e idempotente por pedido_id). Aquí se garantiza que no haya sobreventa.
 * Compensación C2: liberar la reserva.
 */
@Component
public class ReservadorDeStock extends ManejadorPedido {

    private final CatalogoClient catalogo;
    private final RegistroPedidos registro;

    public ReservadorDeStock(CatalogoClient catalogo, RegistroPedidos registro) {
        this.catalogo = catalogo;
        this.registro = registro;
    }

    @Override
    protected void manejar(ContextoPedido ctx) {
        catalogo.reservar(ctx.getPedido().getId(), ctx.getItems().stream()
                .map(i -> new ItemReserva(i.productoId(), i.cantidad()))
                .toList());
        ctx.getPedido().setEstado(EstadoPedido.STOCK_RESERVADO);
        registro.guardar(ctx);
    }

    @Override
    protected boolean esCompensable() {
        return true;
    }

    @Override
    public String compensar(ContextoPedido ctx) {
        ReservaRemota r = catalogo.liberar(ctx.getPedido().getId());
        return "Reserva de stock: " + r.estado();
    }

    @Override
    public String nombre() {
        return "RESERVAR_STOCK";
    }
}
