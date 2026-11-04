package uy.edu.utu.shopexpress.pedidos.cadena;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uy.edu.utu.shopexpress.pedidos.cadena.manejadores.CalculadorDeTotal;
import uy.edu.utu.shopexpress.pedidos.cadena.manejadores.ConfirmadorDePedido;
import uy.edu.utu.shopexpress.pedidos.cadena.manejadores.NotificadorDeHistorial;
import uy.edu.utu.shopexpress.pedidos.cadena.manejadores.ProcesadorDePago;
import uy.edu.utu.shopexpress.pedidos.cadena.manejadores.RegistradorDePedido;
import uy.edu.utu.shopexpress.pedidos.cadena.manejadores.ReservadorDeStock;
import uy.edu.utu.shopexpress.pedidos.cadena.manejadores.ValidadorDeDatos;
import uy.edu.utu.shopexpress.pedidos.cadena.manejadores.ValidadorDeStock;
import uy.edu.utu.shopexpress.pedidos.cadena.manejadores.ValidadorDeUsuario;

/**
 * Arma la cadena en un único lugar, de forma explícita: el orden de los eslabones
 * se lee de arriba hacia abajo y agregar o quitar un paso es cambiar una línea.
 */
@Configuration
public class ConfiguracionCadena {

    @Bean
    public CadenaPedido cadenaPedido(ValidadorDeDatos validadorDeDatos,
                                     ValidadorDeUsuario validadorDeUsuario,
                                     ValidadorDeStock validadorDeStock,
                                     CalculadorDeTotal calculadorDeTotal,
                                     RegistradorDePedido registradorDePedido,
                                     ReservadorDeStock reservadorDeStock,
                                     ProcesadorDePago procesadorDePago,
                                     ConfirmadorDePedido confirmadorDePedido,
                                     NotificadorDeHistorial notificadorDeHistorial) {
        validadorDeDatos
                .enlazar(validadorDeUsuario)
                .enlazar(validadorDeStock)
                .enlazar(calculadorDeTotal)
                .enlazar(registradorDePedido)
                .enlazar(reservadorDeStock)
                .enlazar(procesadorDePago)
                .enlazar(confirmadorDePedido)
                .enlazar(notificadorDeHistorial);
        return new CadenaPedido(validadorDeDatos);
    }
}
