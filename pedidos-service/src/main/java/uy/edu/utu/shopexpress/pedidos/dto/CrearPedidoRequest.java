package uy.edu.utu.shopexpress.pedidos.dto;

import java.util.List;

/**
 * Solicitud de pedido. A propósito NO usa Bean Validation: la validación de datos es
 * responsabilidad del primer eslabón de la cadena (ValidadorDeDatos).
 *
 * Los precios NO vienen del cliente: los fija el catálogo (CalculadorDeTotal).
 */
public record CrearPedidoRequest(String usuarioId, List<Item> items, Pago pago) {

    public record Item(String productoId, Integer cantidad) {
    }

    public record Pago(String metodo, String tokenTarjeta) {
    }
}
