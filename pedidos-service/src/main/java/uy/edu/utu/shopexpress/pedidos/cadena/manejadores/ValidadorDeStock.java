package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.cliente.CatalogoClient;
import uy.edu.utu.shopexpress.pedidos.cliente.CatalogoClient.ProductoRemoto;
import uy.edu.utu.shopexpress.pedidos.dto.CrearPedidoRequest;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;

/**
 * Eslabón 3: verifica que cada producto exista y tenga stock.
 *
 * Es una verificación PREVIA y optimista: lee el catálogo (que puede venir de caché,
 * consistencia eventual) para rechazar rápido los casos obvios sin crear el pedido.
 * La garantía real contra la sobreventa la da después ReservadorDeStock con la
 * operación atómica en MongoDB.
 */
@Component
public class ValidadorDeStock extends ManejadorPedido {

    private final CatalogoClient catalogo;

    public ValidadorDeStock(CatalogoClient catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    protected void manejar(ContextoPedido ctx) {
        for (CrearPedidoRequest.Item item : ctx.getItemsNormalizados()) {
            ProductoRemoto producto = catalogo.obtenerProducto(item.productoId());
            if (producto.stockDisponible() < item.cantidad()) {
                throw new NegocioException(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE",
                        "Stock insuficiente para " + producto.nombre() + " (" + producto.id() + "): solicitado "
                                + item.cantidad() + ", disponible " + producto.stockDisponible());
            }
            ctx.getProductos().put(item.productoId(), producto);
        }
    }

    @Override
    public String nombre() {
        return "VALIDAR_STOCK";
    }
}
