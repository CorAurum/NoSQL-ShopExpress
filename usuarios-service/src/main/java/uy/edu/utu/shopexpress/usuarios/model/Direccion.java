package uy.edu.utu.shopexpress.usuarios.model;

/** Dirección de envío, embebida dentro del documento de usuario. */
public record Direccion(String calle, String ciudad, String codigoPostal) {
}
