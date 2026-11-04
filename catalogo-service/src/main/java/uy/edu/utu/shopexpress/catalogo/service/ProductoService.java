package uy.edu.utu.shopexpress.catalogo.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uy.edu.utu.shopexpress.catalogo.cache.CacheProductos;
import uy.edu.utu.shopexpress.catalogo.dto.CrearProductoRequest;
import uy.edu.utu.shopexpress.catalogo.dto.ProductoResponse;
import uy.edu.utu.shopexpress.catalogo.error.NegocioException;
import uy.edu.utu.shopexpress.catalogo.model.Categoria;
import uy.edu.utu.shopexpress.catalogo.model.Producto;
import uy.edu.utu.shopexpress.catalogo.repository.ProductoRepository;
import uy.edu.utu.shopexpress.catalogo.util.Ids;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class ProductoService {

    private final ProductoRepository repositorio;
    private final MongoTemplate mongo;
    private final CacheProductos cache;

    public ProductoService(ProductoRepository repositorio, MongoTemplate mongo, CacheProductos cache) {
        this.repositorio = repositorio;
        this.mongo = mongo;
        this.cache = cache;
    }

    public ProductoResponse crear(CrearProductoRequest solicitud) {
        return crearConId(Ids.nuevo("prod"), solicitud);
    }

    public ProductoResponse crearConId(String id, CrearProductoRequest solicitud) {
        String sku = solicitud.sku().trim().toUpperCase(Locale.ROOT);
        if (repositorio.existsBySku(sku)) {
            throw new NegocioException(HttpStatus.CONFLICT, "SKU_DUPLICADO", "Ya existe un producto con SKU " + sku);
        }
        Instant ahora = Instant.now();
        Producto p = new Producto();
        p.setId(id);
        p.setSku(sku);
        p.setNombre(solicitud.nombre().trim());
        p.setDescripcion(solicitud.descripcion());
        p.setCategoria(new Categoria(solicitud.categoria().codigo().trim().toUpperCase(Locale.ROOT),
                solicitud.categoria().nombre().trim()));
        p.setPrecio(solicitud.precio().setScale(2, RoundingMode.HALF_UP));
        p.setStockDisponible(solicitud.stockInicial());
        p.setStockReservado(0);
        p.setAtributos(solicitud.atributos() == null ? new HashMap<>() : new HashMap<>(solicitud.atributos()));
        p.setFechaAlta(ahora);
        p.setFechaActualizacion(ahora);
        try {
            return aRespuesta(repositorio.insert(p));
        } catch (DuplicateKeyException e) {
            throw new NegocioException(HttpStatus.CONFLICT, "SKU_DUPLICADO", "Ya existe un producto con SKU " + sku);
        }
    }

    /** Lectura rápida (cache-aside). Puede reflejar stock con algunos segundos de atraso. */
    public ProductoResponse obtener(String id) {
        return cache.obtener(id).orElseGet(() -> {
            ProductoResponse respuesta = aRespuesta(repositorio.findById(id).orElseThrow(() -> noEncontrado(id)));
            cache.guardar(respuesta);
            return respuesta;
        });
    }

    /** Lectura FUERTE: siempre desde MongoDB, sin caché (para auditoría y pruebas de stock). */
    public ProductoResponse obtenerSinCache(String id) {
        return aRespuesta(repositorio.findById(id).orElseThrow(() -> noEncontrado(id)));
    }

    public List<ProductoResponse> listar() {
        return repositorio.findAll().stream().map(ProductoService::aRespuesta).toList();
    }

    /** Ingreso de mercadería: incremento atómico con $inc. */
    public ProductoResponse reponerStock(String id, int cantidad) {
        Producto actualizado = mongo.findAndModify(
                query(where("id").is(id)),
                new Update().inc("stockDisponible", cantidad).set("fechaActualizacion", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                Producto.class);
        if (actualizado == null) {
            throw noEncontrado(id);
        }
        cache.invalidar(List.of(id));
        return aRespuesta(actualizado);
    }

    static ProductoResponse aRespuesta(Producto p) {
        return new ProductoResponse(p.getId(), p.getSku(), p.getNombre(), p.getDescripcion(), p.getCategoria(),
                p.getPrecio(), p.getStockDisponible(), p.getStockReservado(), p.getAtributos(),
                p.getFechaActualizacion());
    }

    static NegocioException noEncontrado(String id) {
        return new NegocioException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO", "No existe el producto " + id);
    }
}
