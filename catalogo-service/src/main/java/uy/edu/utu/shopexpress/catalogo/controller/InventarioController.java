package uy.edu.utu.shopexpress.catalogo.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utu.shopexpress.catalogo.dto.ProductoResponse;
import uy.edu.utu.shopexpress.catalogo.dto.ReservaRequest;
import uy.edu.utu.shopexpress.catalogo.dto.ReservaResponse;
import uy.edu.utu.shopexpress.catalogo.service.InventarioService;
import uy.edu.utu.shopexpress.catalogo.service.ProductoService;

/**
 * Endpoints internos de inventario, invocados por el orquestador SAGA del
 * Servicio de Pedidos. Las operaciones sobre reservas son idempotentes por pedido_id.
 */
@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService servicio;
    private final ProductoService productos;

    public InventarioController(InventarioService servicio, ProductoService productos) {
        this.servicio = servicio;
        this.productos = productos;
    }

    /** Stock leído directamente de MongoDB (consistencia fuerte, sin caché). */
    @GetMapping("/stock/{productoId}")
    public ProductoResponse stock(@PathVariable("productoId") String productoId) {
        return productos.obtenerSinCache(productoId);
    }

    @PostMapping("/reservas")
    public ReservaResponse reservar(@Valid @RequestBody ReservaRequest solicitud) {
        return servicio.reservar(solicitud);
    }

    @GetMapping("/reservas/{pedidoId}")
    public ReservaResponse obtener(@PathVariable("pedidoId") String pedidoId) {
        return servicio.obtener(pedidoId);
    }

    @PostMapping("/reservas/{pedidoId}/confirmar")
    public ReservaResponse confirmar(@PathVariable("pedidoId") String pedidoId) {
        return servicio.confirmar(pedidoId);
    }

    @PostMapping("/reservas/{pedidoId}/liberar")
    public ReservaResponse liberar(@PathVariable("pedidoId") String pedidoId) {
        return servicio.liberar(pedidoId);
    }
}
