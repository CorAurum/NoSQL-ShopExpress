package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.dto.CrearPedidoRequest;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Eslabón 1: valida la forma de la solicitud sin consultar a nadie (falla rápido y barato)
 * y normaliza los ítems uniendo líneas repetidas del mismo producto.
 */
@Component
public class ValidadorDeDatos extends ManejadorPedido {

    public static final Set<String> METODOS_PAGO = Set.of("TARJETA_CREDITO", "TARJETA_DEBITO", "TRANSFERENCIA");
    static final int MAX_PRODUCTOS = 20;
    static final int MAX_CANTIDAD = 50;

    @Override
    protected void manejar(ContextoPedido ctx) {
        CrearPedidoRequest s = ctx.getSolicitud();
        List<String> errores = new ArrayList<>();

        if (s == null) {
            throw invalido(List.of("Falta el cuerpo de la solicitud"));
        }
        if (s.usuarioId() == null || s.usuarioId().isBlank()) {
            errores.add("usuario_id es obligatorio");
        }

        Map<String, Integer> agrupados = new LinkedHashMap<>();
        if (s.items() == null || s.items().isEmpty()) {
            errores.add("items debe contener al menos un producto");
        } else {
            for (int i = 0; i < s.items().size(); i++) {
                CrearPedidoRequest.Item item = s.items().get(i);
                if (item == null || item.productoId() == null || item.productoId().isBlank()) {
                    errores.add("items[" + i + "].producto_id es obligatorio");
                    continue;
                }
                if (item.cantidad() == null || item.cantidad() < 1) {
                    errores.add("items[" + i + "].cantidad debe ser mayor a 0");
                    continue;
                }
                agrupados.merge(item.productoId().trim(), item.cantidad(), Integer::sum);
            }
            agrupados.forEach((producto, cantidad) -> {
                if (cantidad > MAX_CANTIDAD) {
                    errores.add("La cantidad de " + producto + " no puede superar " + MAX_CANTIDAD);
                }
            });
            if (agrupados.size() > MAX_PRODUCTOS) {
                errores.add("Un pedido no puede tener más de " + MAX_PRODUCTOS + " productos distintos");
            }
        }

        if (s.pago() == null) {
            errores.add("pago es obligatorio");
        } else if (s.pago().metodo() == null || !METODOS_PAGO.contains(s.pago().metodo())) {
            errores.add("pago.metodo debe ser uno de " + METODOS_PAGO);
        } else if (s.pago().metodo().startsWith("TARJETA")
                && (s.pago().tokenTarjeta() == null || s.pago().tokenTarjeta().isBlank())) {
            errores.add("pago.token_tarjeta es obligatorio para pagos con tarjeta");
        }

        if (!errores.isEmpty()) {
            throw invalido(errores);
        }

        ctx.setItemsNormalizados(agrupados.entrySet().stream()
                .map(e -> new CrearPedidoRequest.Item(e.getKey(), e.getValue()))
                .toList());
    }

    private static NegocioException invalido(List<String> errores) {
        return new NegocioException(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS",
                "La solicitud de pedido no es válida", errores);
    }

    @Override
    public String nombre() {
        return "VALIDAR_DATOS";
    }
}
