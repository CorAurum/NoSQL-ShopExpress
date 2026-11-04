package uy.edu.utu.shopexpress.catalogo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Documento de producto (colección "productos").
 *
 * Modelado:
 *  - categoria: EMBEBIDA (desnormalizada). Se lee en cada consulta y casi no cambia,
 *    así evitamos un segundo acceso.
 *  - atributos: mapa flexible (talle, color, voltaje...) que varía por categoría; un
 *    esquema flexible es una de las razones para usar un modelo documental.
 *  - stock_disponible y stock_reservado viven DENTRO del documento del producto:
 *    MongoDB garantiza atomicidad a nivel de documento, por lo que "verificar stock y
 *    descontarlo" se resuelve en una única operación condicional, sin sobreventa.
 */
@Document("productos")
public class Producto {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sku;

    private String nombre;

    private String descripcion;

    private Categoria categoria;

    private BigDecimal precio;

    private int stockDisponible;

    private int stockReservado;

    private Map<String, String> atributos = new HashMap<>();

    private Instant fechaAlta;

    private Instant fechaActualizacion;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public int getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(int stockDisponible) { this.stockDisponible = stockDisponible; }

    public int getStockReservado() { return stockReservado; }
    public void setStockReservado(int stockReservado) { this.stockReservado = stockReservado; }

    public Map<String, String> getAtributos() { return atributos; }
    public void setAtributos(Map<String, String> atributos) { this.atributos = atributos; }

    public Instant getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(Instant fechaAlta) { this.fechaAlta = fechaAlta; }

    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(Instant fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
