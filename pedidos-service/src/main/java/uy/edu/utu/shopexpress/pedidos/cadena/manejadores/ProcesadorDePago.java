package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;
import uy.edu.utu.shopexpress.pedidos.model.EstadoPedido;
import uy.edu.utu.shopexpress.pedidos.model.InfoPago;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;
import uy.edu.utu.shopexpress.pedidos.model.TransaccionPago;
import uy.edu.utu.shopexpress.pedidos.pago.PasarelaPagoSimulada;
import uy.edu.utu.shopexpress.pedidos.saga.RegistroPedidos;

/**
 * Eslabón 7 / Paso SAGA T3: cobra el total en la pasarela (idempotente por pedido_id).
 * Compensación C3: reembolsar.
 */
@Component
public class ProcesadorDePago extends ManejadorPedido {

    private final PasarelaPagoSimulada pasarela;
    private final RegistroPedidos registro;

    public ProcesadorDePago(PasarelaPagoSimulada pasarela, RegistroPedidos registro) {
        this.pasarela = pasarela;
        this.registro = registro;
    }

    @Override
    protected void manejar(ContextoPedido ctx) {
        Pedido pedido = ctx.getPedido();
        String metodo = ctx.getSolicitud().pago().metodo();
        TransaccionPago tx = pasarela.cobrar(pedido.getId(), pedido.getTotal(), metodo,
                ctx.getSolicitud().pago().tokenTarjeta());

        if (tx.getEstado() != TransaccionPago.Estado.APROBADA) {
            pedido.setPago(new InfoPago(metodo, "RECHAZADO", tx.getTransaccionId(), tx.getMotivo()));
            throw new NegocioException(HttpStatus.PAYMENT_REQUIRED, "PAGO_RECHAZADO", tx.getMotivo());
        }

        pedido.setPago(new InfoPago(metodo, "APROBADO", tx.getTransaccionId(), null));
        pedido.setEstado(EstadoPedido.PAGADO);
        registro.guardar(ctx);
    }

    @Override
    protected boolean esCompensable() {
        return true;
    }

    @Override
    public String compensar(ContextoPedido ctx) {
        Pedido pedido = ctx.getPedido();
        return pasarela.reembolsar(pedido.getId())
                .filter(tx -> tx.getEstado() == TransaccionPago.Estado.REEMBOLSADA)
                .map(tx -> {
                    pedido.setPago(new InfoPago(tx.getMetodo(), "REEMBOLSADO", tx.getTransaccionId(), null));
                    return "Pago " + tx.getTransaccionId() + " reembolsado";
                })
                .orElse("No había cobro aprobado que reembolsar");
    }

    @Override
    public String nombre() {
        return "PROCESAR_PAGO";
    }
}
