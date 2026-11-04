package uy.edu.utu.shopexpress.pedidos.cadena;

/** Punto de entrada de la cadena: envuelve al primer eslabón. */
public record CadenaPedido(ManejadorPedido primero) {

    public void procesar(ContextoPedido ctx) {
        primero.procesar(ctx);
    }
}
