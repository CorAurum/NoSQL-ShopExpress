package uy.edu.utu.shopexpress.catalogo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.catalogo.dto.CategoriaRequest;
import uy.edu.utu.shopexpress.catalogo.dto.CrearProductoRequest;
import uy.edu.utu.shopexpress.catalogo.repository.ProductoRepository;
import uy.edu.utu.shopexpress.catalogo.service.ProductoService;

import java.math.BigDecimal;
import java.util.Map;

/** Productos de ejemplo con IDs fijos para la demo. */
@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true")
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final ProductoRepository repositorio;
    private final ProductoService servicio;

    public DatosIniciales(ProductoRepository repositorio, ProductoService servicio) {
        this.repositorio = repositorio;
        this.servicio = servicio;
    }

    @Override
    public void run(String... args) {
        crearSiFalta("prod_001", new CrearProductoRequest("AUR-BT-001", "Auriculares Bluetooth",
                "Auriculares inalámbricos con cancelación de ruido",
                new CategoriaRequest("AUDIO", "Audio"), new BigDecimal("25.50"), 100,
                Map.of("color", "negro", "autonomia", "30h")));
        crearSiFalta("prod_002", new CrearProductoRequest("TEC-MEC-002", "Teclado mecánico",
                "Teclado mecánico switches rojos", new CategoriaRequest("PERIF", "Periféricos"),
                new BigDecimal("89.90"), 3, Map.of("layout", "ES")));
        crearSiFalta("prod_003", new CrearProductoRequest("MON-27-003", "Monitor 27\"",
                "Monitor IPS 27 pulgadas 144Hz", new CategoriaRequest("MONITOR", "Monitores"),
                new BigDecimal("349.00"), 0, Map.of("resolucion", "2560x1440")));
    }

    private void crearSiFalta(String id, CrearProductoRequest solicitud) {
        if (repositorio.existsById(id)) {
            return;
        }
        try {
            servicio.crearConId(id, solicitud);
            log.info("Producto semilla {} creado", id);
        } catch (RuntimeException e) {
            log.warn("No se cargó el producto semilla {}: {}", id, e.getMessage());
        }
    }
}
