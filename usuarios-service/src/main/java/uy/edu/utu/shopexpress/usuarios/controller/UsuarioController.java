package uy.edu.utu.shopexpress.usuarios.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utu.shopexpress.usuarios.dto.CrearUsuarioRequest;
import uy.edu.utu.shopexpress.usuarios.dto.RegistrarCompraRequest;
import uy.edu.utu.shopexpress.usuarios.dto.UsuarioExportado;
import uy.edu.utu.shopexpress.usuarios.dto.UsuarioResponse;
import uy.edu.utu.shopexpress.usuarios.service.ExportadorCsv;
import uy.edu.utu.shopexpress.usuarios.service.UsuarioService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest solicitud) {
        UsuarioResponse creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.id())).body(creado);
    }

    /** Exporta todos los usuarios anonimizados. ?formato=json (por defecto) o ?formato=csv */
    @GetMapping("/exportar")
    public ResponseEntity<?> exportar(@RequestParam(name = "formato", defaultValue = "json") String formato) {
        List<UsuarioExportado> datos = servicio.exportarAnonimizado();
        if ("csv".equalsIgnoreCase(formato)) {
            return ResponseEntity.ok()
                    .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=usuarios_anonimizados.csv")
                    .body(ExportadorCsv.aCsv(datos));
        }
        return ResponseEntity.ok(datos);
    }

    @GetMapping("/{usuarioId}")
    public UsuarioResponse obtener(@PathVariable("usuarioId") String usuarioId) {
        return servicio.obtener(usuarioId);
    }

    /** Endpoint interno: lo invoca el Servicio de Pedidos al confirmar una compra. */
    @PostMapping("/{usuarioId}/compras")
    public UsuarioResponse registrarCompra(@PathVariable("usuarioId") String usuarioId,
                                           @Valid @RequestBody RegistrarCompraRequest solicitud) {
        return servicio.registrarCompra(usuarioId, solicitud);
    }
}
