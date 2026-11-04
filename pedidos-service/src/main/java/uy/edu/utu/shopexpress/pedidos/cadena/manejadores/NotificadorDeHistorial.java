package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.cliente.UsuariosClient;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;
import uy.edu.utu.shopexpress.pedidos.model.ResultadoPaso;

/**
 * Eslabón 9 (posterior al pivote): agrega el pedido al historial de compras del usuario.
 *
 * Es "best effort": la venta ya está confirmada, así que un fallo aquí NO debe deshacerla.
 * Se registra como PENDIENTE_REINTENTO; como el endpoint de usuarios es idempotente, puede
 * reintentarse más tarde sin duplicar (en producción lo haría un proceso de reintentos o
 * un evento en una cola).
 */
@Component
public class NotificadorDeHistorial extends ManejadorPedido {

    private static final Logger log = LoggerFactory.getLogger(NotificadorDeHistorial.class);

    private final UsuariosClient usuarios;

    public NotificadorDeHistorial(UsuariosClient usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void manejar(ContextoPedido ctx) {
        Pedido p = ctx.getPedido();
        int unidades = p.getItems().stream().mapToInt(i -> i.cantidad()).sum();
        try {
            usuarios.registrarCompra(p.getUsuarioId(), p.getId(), p.getTotal(), unidades);
            ctx.registrarPasoOk(nombre());
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el pedido {} en el historial del usuario: {}", p.getId(), e.getMessage());
            ctx.registrarPaso(nombre(), ResultadoPaso.PENDIENTE_REINTENTO, e.getMessage());
        }
    }

    @Override
    public String nombre() {
        return "REGISTRAR_HISTORIAL";
    }
}
