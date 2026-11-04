package uy.edu.utu.shopexpress.pedidos.saga;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uy.edu.utu.shopexpress.pedidos.cadena.CadenaPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.dto.CrearPedidoRequest;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Verifica la mecánica SAGA + Chain of Responsibility sin levantar Spring ni bases de datos. */
class OrquestadorSagaPedidosTest {

    private final List<String> eventos = new ArrayList<>();

    private class PasoFalso extends ManejadorPedido {
        private final String nombre;
        private final boolean compensable;
        private final boolean falla;

        PasoFalso(String nombre, boolean compensable, boolean falla) {
            this.nombre = nombre;
            this.compensable = compensable;
            this.falla = falla;
        }

        @Override
        protected void manejar(ContextoPedido ctx) {
            eventos.add("ejecutar:" + nombre);
            if (falla) {
                throw new NegocioException(HttpStatus.PAYMENT_REQUIRED, "PAGO_RECHAZADO", "rechazado");
            }
        }

        @Override
        public String compensar(ContextoPedido ctx) {
            eventos.add("compensar:" + nombre);
            return "ok";
        }

        @Override
        protected boolean esCompensable() {
            return compensable;
        }

        @Override
        public String nombre() {
            return nombre;
        }
    }

    private OrquestadorSagaPedidos orquestador(ManejadorPedido primero) {
        return new OrquestadorSagaPedidos(new CadenaPedido(primero), mock(RegistroPedidos.class),
                mock(ControlIdempotencia.class));
    }

    private static CrearPedidoRequest solicitud() {
        return new CrearPedidoRequest("usr_1", List.of(new CrearPedidoRequest.Item("prod_1", 1)),
                new CrearPedidoRequest.Pago("TRANSFERENCIA", null));
    }

    @Test
    void ejecutaLosPasosEnOrdenYCompensaEnOrdenInverso() {
        PasoFalso validar = new PasoFalso("VALIDAR", false, false);
        PasoFalso reservar = new PasoFalso("RESERVAR", true, false);
        PasoFalso cobrar = new PasoFalso("COBRAR", true, true);
        PasoFalso confirmar = new PasoFalso("CONFIRMAR", false, false);
        validar.enlazar(reservar).enlazar(cobrar).enlazar(confirmar);

        ResultadoPedido resultado = orquestador(validar).crearPedido(solicitud(), null, null);

        // CONFIRMAR nunca se ejecuta; VALIDAR no es compensable. COBRAR se compensa porque
        // se registra antes de ejecutarse (resultado potencialmente incierto).
        assertThat(eventos).containsExactly(
                "ejecutar:VALIDAR", "ejecutar:RESERVAR", "ejecutar:COBRAR",
                "compensar:COBRAR", "compensar:RESERVAR");
        assertThat(resultado.error().getCodigo()).isEqualTo("PAGO_RECHAZADO");
        assertThat(resultado.pedido()).isNull();
    }

    @Test
    void sinFallosNoHayCompensaciones() {
        PasoFalso a = new PasoFalso("A", true, false);
        PasoFalso b = new PasoFalso("B", true, false);
        a.enlazar(b);

        ResultadoPedido resultado = orquestador(a).crearPedido(solicitud(), null, null);

        assertThat(eventos).containsExactly("ejecutar:A", "ejecutar:B");
        assertThat(resultado.error()).isNull();
    }
}
