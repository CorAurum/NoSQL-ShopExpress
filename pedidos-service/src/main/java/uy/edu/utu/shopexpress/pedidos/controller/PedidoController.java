package uy.edu.utu.shopexpress.pedidos.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uy.edu.utu.shopexpress.pedidos.dto.CrearPedidoRequest;
import uy.edu.utu.shopexpress.pedidos.dto.ErrorPedido;
import uy.edu.utu.shopexpress.pedidos.error.NegocioException;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;
import uy.edu.utu.shopexpress.pedidos.repository.PedidoRepository;
import uy.edu.utu.shopexpress.pedidos.saga.OrquestadorSagaPedidos;
import uy.edu.utu.shopexpress.pedidos.saga.ResultadoPedido;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final OrquestadorSagaPedidos orquestador;
    private final PedidoRepository pedidos;

    public PedidoController(OrquestadorSagaPedidos orquestador, PedidoRepository pedidos) {
        this.orquestador = orquestador;
        this.pedidos = pedidos;
    }

    /**
     * Inicia la SAGA y la Chain of Responsibility.
     *
     * Cabeceras opcionales:
     *  - Idempotency-Key: evita pedidos duplicados ante reintentos.
     *  - X-Simular-Fallo: CONFIRMACION -> fuerza un fallo después del cobro (demo de compensaciones).
     *
     * Respuestas: 201 confirmado | 200 repetido (idempotencia) | 400 datos | 402 pago rechazado |
     *             404 usuario/producto | 409 stock insuficiente | 503 servicio caído | 500 fallo.
     */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearPedidoRequest solicitud,
                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                   @RequestHeader(value = "X-Simular-Fallo", required = false) String simularFallo) {
        ResultadoPedido resultado = orquestador.crearPedido(solicitud, idempotencyKey, simularFallo);

        if (resultado.repetido()) {
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(resultado.pedido());
        }
        if (resultado.error() == null) {
            return ResponseEntity.created(URI.create("/api/pedidos/" + resultado.pedido().getId()))
                    .body(resultado.pedido());
        }
        NegocioException e = resultado.error();
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorPedido(e.getCodigo(), e.getMessage(), e.getDetalles(), resultado.pedido(), Instant.now()));
    }

    @GetMapping("/{pedidoId}")
    public Pedido obtener(@PathVariable("pedidoId") String pedidoId) {
        return pedidos.findById(pedidoId).orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                "PEDIDO_NO_ENCONTRADO", "No existe el pedido " + pedidoId));
    }

    @GetMapping
    public List<Pedido> listarPorUsuario(@RequestParam("usuario_id") String usuarioId) {
        return pedidos.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }
}
