package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.cliente.CatalogoClient.ProductoRemoto;
import uy.edu.utu.shopexpress.pedidos.model.ItemPedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Eslabón 4: calcula subtotales y total con los precios del catálogo (nunca los del cliente). */
@Component
public class CalculadorDeTotal extends ManejadorPedido {

    @Override
    protected void manejar(ContextoPedido ctx) {
        List<ItemPedido> items = ctx.getItemsNormalizados().stream()
                .map(item -> {
                    ProductoRemoto p = ctx.getProductos().get(item.productoId());
                    BigDecimal precio = p.precio().setScale(2, RoundingMode.HALF_UP);
                    BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.cantidad()))
                            .setScale(2, RoundingMode.HALF_UP);
                    return new ItemPedido(p.id(), p.nombre(), item.cantidad(), precio, subtotal);
                })
                .toList();

        ctx.setItems(items);
        ctx.setTotal(items.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    public String nombre() {
        return "CALCULAR_TOTAL";
    }
}
