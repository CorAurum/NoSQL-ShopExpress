package uy.edu.utu.shopexpress.pedidos.cadena;

/**
 * Eslabón abstracto del patrón CHAIN OF RESPONSIBILITY.
 *
 * Cada manejador resuelve UNA responsabilidad sobre la solicitud de pedido y, si todo
 * está bien, la pasa al siguiente. Para cortar la cadena lanza una NegocioException.
 *
 * Integración con la SAGA: los manejadores que modifican estado en otro servicio
 * (reservar stock, cobrar) son "compensables". Se registran en la pila de
 * compensaciones del contexto ANTES de ejecutarse: si la llamada termina con un
 * resultado incierto (p. ej. timeout después de que el otro servicio aplicó el cambio),
 * igual se intentará deshacer. Por eso todas las compensaciones son idempotentes.
 */
public abstract class ManejadorPedido {

    private ManejadorPedido siguiente;

    /** Enlaza el siguiente eslabón y lo devuelve, para escribir a.enlazar(b).enlazar(c). */
    public ManejadorPedido enlazar(ManejadorPedido siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void procesar(ContextoPedido ctx) {
        ctx.setPasoEnCurso(nombre());
        if (esCompensable()) {
            ctx.registrarCompensable(this);
        }
        int pasosAntes = ctx.getPasos().size();
        manejar(ctx);
        if (ctx.getPasos().size() == pasosAntes) {
            ctx.registrarPasoOk(nombre()); // salvo que el manejador haya registrado su propio resultado
        }
        if (siguiente != null) {
            siguiente.procesar(ctx);
        }
    }

    /** Lógica propia del eslabón. */
    protected abstract void manejar(ContextoPedido ctx);

    /** Transacción de compensación. Devuelve una descripción de lo que se deshizo. */
    public String compensar(ContextoPedido ctx) {
        return "Sin acción";
    }

    protected boolean esCompensable() {
        return false;
    }

    /** Nombre del paso en la bitácora de la SAGA. */
    public abstract String nombre();
}
