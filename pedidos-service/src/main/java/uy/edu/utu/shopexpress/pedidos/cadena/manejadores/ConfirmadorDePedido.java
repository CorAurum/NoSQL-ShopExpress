package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.cliente.CatalogoClient;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;
import uy.edu.utu.shopexpress.pedidos.model.EstadoPedido;
import uy.edu.utu.shopexpress.pedidos.saga.RegistroPedidos;

/**
 * Eslabón 8 / Paso SAGA T4 (paso pivote): confirma la reserva y cierra el pedido.
 * Una vez confirmado, la SAGA ya no se revierte, por eso no tiene compensación propia.
 *
 * Para la demo, la cabecera "X-Simular-Fallo: CONFIRMACION" fuerza un fallo aquí y
 * permite ver la cadena completa de compensaciones (reembolso + liberación de stock).
 */
@Component
public class ConfirmadorDePedido extends ManejadorPedido {

    private final CatalogoClient catalogo;
    private final RegistroPedidos registro;

    public ConfirmadorDePedido(CatalogoClient catalogo, RegistroPedidos registro) {
        this.catalogo = catalogo;
        this.registro = registro;
    }

    @Override
    protected void manejar(ContextoPedido ctx) {
        if (ctx.debeFallarEn("CONFIRMACION")) {
            throw new NegocioException(HttpStatus.INTERNAL_SERVER_ERROR, "FALLO_SIMULADO",
                    "Fallo simulado en la confirmación (cabecera X-Simular-Fallo)");
        }
        catalogo.confirmar(ctx.getPedido().getId());
        ctx.getPedido().setEstado(EstadoPedido.CONFIRMADO);
        registro.guardar(ctx);
    }

    @Override
    public String nombre() {
        return "CONFIRMAR_PEDIDO";
    }
}
