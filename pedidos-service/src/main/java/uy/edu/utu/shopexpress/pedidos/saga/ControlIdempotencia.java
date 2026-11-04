package uy.edu.utu.shopexpress.pedidos.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;
import uy.edu.utu.shopexpress.pedidos.repository.PedidoRepository;

import java.time.Duration;
import java.util.Optional;

/**
 * Garantiza que "un pedido sea único" ante reintentos del cliente (doble clic, red
 * inestable). El cliente envía la cabecera Idempotency-Key:
 *
 *  - Redis SET NX (atómico) toma un candado "EN_PROCESO" para esa clave: si llegan dos
 *    solicitudes iguales a la vez, solo una pasa; la otra recibe 409.
 *  - Al terminar, la clave apunta al pedido_id durante 24 h: repetir la solicitud
 *    devuelve el MISMO pedido en lugar de crear otro.
 *  - Si Redis no está disponible, el índice único de MongoDB sobre idempotency_key
 *    sigue impidiendo duplicados (Redis acelera, Mongo garantiza).
 */
@Component
public class ControlIdempotencia {

    private static final Logger log = LoggerFactory.getLogger(ControlIdempotencia.class);
    private static final String PREFIJO = "idempotencia:pedidos:";
    private static final String EN_PROCESO = "EN_PROCESO";
    private static final Duration TTL_CANDADO = Duration.ofMinutes(5);
    private static final Duration TTL_RESULTADO = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final PedidoRepository pedidos;

    public ControlIdempotencia(StringRedisTemplate redis, PedidoRepository pedidos) {
        this.redis = redis;
        this.pedidos = pedidos;
    }

    /** Pedido ya creado para esta clave, si existe. */
    public Optional<Pedido> buscarPedidoPrevio(String clave) {
        String valor = leer(clave);
        if (EN_PROCESO.equals(valor)) {
            return Optional.empty();
        }
        if (valor != null) {
            return pedidos.findById(valor);
        }
        return pedidos.findByIdempotencyKey(clave); // respaldo si Redis perdió o no tiene la clave
    }

    /** Intenta tomar el candado. false = otra solicitud con la misma clave está en curso. */
    public boolean adquirir(String clave) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(PREFIJO + clave, EN_PROCESO, TTL_CANDADO));
        } catch (RuntimeException e) {
            log.warn("Redis no disponible ({}); se confía en el índice único de MongoDB", e.getMessage());
            return true;
        }
    }

    public void completar(String clave, String pedidoId) {
        if (clave == null) {
            return;
        }
        try {
            redis.opsForValue().set(PREFIJO + clave, pedidoId, TTL_RESULTADO);
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar el resultado de idempotencia ({})", e.getMessage());
        }
    }

    /** Libera el candado cuando la solicitud falló antes de crear el pedido (puede reintentarse). */
    public void liberar(String clave) {
        if (clave == null) {
            return;
        }
        try {
            redis.delete(PREFIJO + clave);
        } catch (RuntimeException e) {
            log.warn("No se pudo liberar el candado de idempotencia ({})", e.getMessage());
        }
    }

    private String leer(String clave) {
        try {
            return redis.opsForValue().get(PREFIJO + clave);
        } catch (RuntimeException e) {
            log.warn("Redis no disponible ({})", e.getMessage());
            return null;
        }
    }
}
