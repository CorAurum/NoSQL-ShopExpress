package uy.edu.utu.shopexpress.usuarios.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento de usuario en MongoDB (colección "usuarios").
 *
 * Modelado:
 *  - direcciones: EMBEBIDAS (relación 1 a pocos, se leen siempre junto al usuario).
 *  - historialCompras: REFERENCIA EXTENDIDA. Solo se guarda pedido_id + un resumen;
 *    el pedido completo vive en el Servicio de Pedidos (su dueño).
 *  - nro_documento: se guarda CIFRADO (AES-256-GCM). Para garantizar unicidad sin
 *    descifrar se guarda además un HMAC determinístico (documento_hash).
 */
@Document("usuarios")
public class Usuario {

    @Id
    private String id;

    private String tipoDocumento;

    private String nroDocumentoCifrado;

    @Indexed(unique = true)
    private String documentoHash;

    private String nombre;

    private String apellido;

    @Indexed(unique = true)
    private String email;

    private List<Direccion> direcciones = new ArrayList<>();

    private List<ResumenCompra> historialCompras = new ArrayList<>();

    private Instant fechaAlta;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNroDocumentoCifrado() { return nroDocumentoCifrado; }
    public void setNroDocumentoCifrado(String nroDocumentoCifrado) { this.nroDocumentoCifrado = nroDocumentoCifrado; }

    public String getDocumentoHash() { return documentoHash; }
    public void setDocumentoHash(String documentoHash) { this.documentoHash = documentoHash; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<Direccion> getDirecciones() { return direcciones; }
    public void setDirecciones(List<Direccion> direcciones) { this.direcciones = direcciones; }

    public List<ResumenCompra> getHistorialCompras() { return historialCompras; }
    public void setHistorialCompras(List<ResumenCompra> historialCompras) { this.historialCompras = historialCompras; }

    public Instant getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(Instant fechaAlta) { this.fechaAlta = fechaAlta; }
}
