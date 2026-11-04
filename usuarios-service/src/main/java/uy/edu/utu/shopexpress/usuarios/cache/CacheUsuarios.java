package uy.edu.utu.shopexpress.usuarios.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.usuarios.dto.UsuarioResponse;

import java.time.Duration;
import java.util.Optional;

/**
 * Caché cache-aside en Redis para las lecturas de usuarios.
 *
 * Prioriza DISPONIBILIDAD: si Redis no responde, se registra una advertencia y la
 * lectura continúa contra MongoDB en vez de fallar. La consistencia es EVENTUAL:
 * una entrada puede quedar desactualizada hasta su TTL, aunque cada escritura propia
 * del servicio la invalida inmediatamente.
 */
@Component
public class CacheUsuarios {

    private static final Logger log = LoggerFactory.getLogger(CacheUsuarios.class);
    private static final String PREFIJO = "usuarios:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    public CacheUsuarios(StringRedisTemplate redis, ObjectMapper mapper,
                         @Value("${app.cache.ttl-segundos:60}") long ttlSegundos) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttl = Duration.ofSeconds(ttlSegundos);
    }

    public Optional<UsuarioResponse> obtener(String id) {
        try {
            String json = redis.opsForValue().get(PREFIJO + id);
            return json == null ? Optional.empty() : Optional.of(mapper.readValue(json, UsuarioResponse.class));
        } catch (Exception e) {
            log.warn("Caché no disponible al leer {} ({}). Se lee desde MongoDB.", id, e.getMessage());
            return Optional.empty();
        }
    }

    public void guardar(UsuarioResponse usuario) {
        try {
            redis.opsForValue().set(PREFIJO + usuario.id(), mapper.writeValueAsString(usuario), ttl);
        } catch (Exception e) {
            log.warn("No se pudo guardar {} en caché ({})", usuario.id(), e.getMessage());
        }
    }

    public void invalidar(String id) {
        try {
            redis.delete(PREFIJO + id);
        } catch (Exception e) {
            log.warn("No se pudo invalidar {} en caché ({}). Expirará por TTL.", id, e.getMessage());
        }
    }
}
