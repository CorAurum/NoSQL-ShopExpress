package uy.edu.utu.shopexpress.catalogo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utu.shopexpress.catalogo.dto.CrearProductoRequest;
import uy.edu.utu.shopexpress.catalogo.dto.ProductoResponse;
import uy.edu.utu.shopexpress.catalogo.dto.ReponerStockRequest;
import uy.edu.utu.shopexpress.catalogo.service.ProductoService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody CrearProductoRequest solicitud) {
        ProductoResponse creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/productos/" + creado.id())).body(creado);
    }

    @GetMapping("/{productoId}")
    public ProductoResponse obtener(@PathVariable("productoId") String productoId) {
        return servicio.obtener(productoId);
    }

    @GetMapping
    public List<ProductoResponse> listar() {
        return servicio.listar();
    }

    /** Ingreso de mercadería (útil para la demo). */
    @PostMapping("/{productoId}/stock")
    public ProductoResponse reponer(@PathVariable("productoId") String productoId,
                                    @Valid @RequestBody ReponerStockRequest solicitud) {
        return servicio.reponerStock(productoId, solicitud.cantidad());
    }
}
