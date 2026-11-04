package uy.edu.utu.shopexpress.catalogo.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.catalogo.dto.ProductoResponse;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;

/**
 * Caché cache-aside en Redis para el catálogo (lado AP del sistema).
 *
 * Las lecturas pueden devolver un stock levemente desactualizado (consistencia
 * eventual, como máximo hasta el TTL). Esto es aceptable porque el stock que se
 * MUESTRA no es el que DECIDE: la reserva siempre se valida contra MongoDB con una
 * operación atómica. Si Redis cae, el servicio sigue respondiendo desde MongoDB.
 */
@Component
public class CacheProductos {

    private static final Logger log = LoggerFactory.getLogger(CacheProductos.class);
    private static final String PREFIJO = "productos:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    public CacheProductos(StringRedisTemplate redis, ObjectMapper mapper,
                          @Value("${app.cache.ttl-segundos:30}") long ttlSegundos) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttl = Duration.ofSeconds(ttlSegundos);
    }

    public Optional<ProductoResponse> obtener(String id) {
        try {
            String json = redis.opsForValue().get(PREFIJO + id);
            return json == null ? Optional.empty() : Optional.of(mapper.readValue(json, ProductoResponse.class));
        } catch (Exception e) {
            log.warn("Caché no disponible al leer {} ({}). Se lee desde MongoDB.", id, e.getMessage());
            return Optional.empty();
        }
    }

    public void guardar(ProductoResponse producto) {
        try {
            redis.opsForValue().set(PREFIJO + producto.id(), mapper.writeValueAsString(producto), ttl);
        } catch (Exception e) {
            log.warn("No se pudo guardar {} en caché ({})", producto.id(), e.getMessage());
        }
    }

    public void invalidar(Collection<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        try {
            redis.delete(ids.stream().map(id -> PREFIJO + id).toList());
        } catch (Exception e) {
            log.warn("No se pudo invalidar caché de {} ({}). Expirará por TTL.", ids, e.getMessage());
        }
    }
}
