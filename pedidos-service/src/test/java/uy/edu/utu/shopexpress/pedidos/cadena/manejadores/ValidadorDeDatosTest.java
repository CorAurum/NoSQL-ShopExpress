package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.junit.jupiter.api.Test;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.dto.CrearPedidoRequest;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorDeDatosTest {

    private final ValidadorDeDatos validador = new ValidadorDeDatos();

    @Test
    void agrupaLineasDelMismoProducto() {
        ContextoPedido ctx = new ContextoPedido(new CrearPedidoRequest("usr_1",
                List.of(new CrearPedidoRequest.Item("prod_1", 2), new CrearPedidoRequest.Item("prod_1", 3)),
                new CrearPedidoRequest.Pago("TARJETA_CREDITO", "tok_ok")), null, null);

        validador.procesar(ctx);

        assertThat(ctx.getItemsNormalizados()).containsExactly(new CrearPedidoRequest.Item("prod_1", 5));
    }

    @Test
    void rechazaDatosInvalidosConTodosLosErrores() {
        ContextoPedido ctx = new ContextoPedido(new CrearPedidoRequest("",
                List.of(new CrearPedidoRequest.Item("prod_1", 0)),
                new CrearPedidoRequest.Pago("TARJETA_CREDITO", null)), null, null);

        NegocioException e = assertThrows(NegocioException.class, () -> validador.procesar(ctx));

        assertThat(e.getCodigo()).isEqualTo("DATOS_INVALIDOS");
        assertThat(e.getDetalles()).hasSize(3);
    }
}
