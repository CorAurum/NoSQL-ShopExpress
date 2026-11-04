package uy.edu.utu.shopexpress.pedidos.pago;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import uy.edu.utu.shopexpress.pedidos.model.TransaccionPago;
import uy.edu.utu.shopexpress.pedidos.repository.TransaccionPagoRepository;
import uy.edu.utu.shopexpress.pedidos.util.Ids;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Pasarela de pago SIMULADA (no hay integración real). Reglas para la demo:
 *  - token_tarjeta = "tok_rechazada"  -> rechazo del emisor.
 *  - monto > pagos.limite-aprobacion   -> rechazo por límite.
 *  - en cualquier otro caso            -> aprobado.
 *
 * Cobro y reembolso son IDEMPOTENTES por pedido_id (el _id de la transacción).
 */
@Service
public class PasarelaPagoSimulada {

    public static final String TOKEN_RECHAZO = "tok_rechazada";

    private final TransaccionPagoRepository repositorio;
    private final MongoTemplate mongo;
    private final BigDecimal limite;

    public PasarelaPagoSimulada(TransaccionPagoRepository repositorio, MongoTemplate mongo,
                                @Value("${pagos.limite-aprobacion}") BigDecimal limite) {
        this.repositorio = repositorio;
        this.mongo = mongo;
        this.limite = limite;
    }

    public TransaccionPago cobrar(String pedidoId, BigDecimal monto, String metodo, String token) {
        Optional<TransaccionPago> previa = repositorio.findById(pedidoId);
        if (previa.isPresent()) {
            return previa.get(); // reintento: no se cobra dos veces
        }

        TransaccionPago tx = new TransaccionPago();
        tx.setId(pedidoId);
        tx.setTransaccionId(Ids.nuevo("tx"));
        tx.setMonto(monto);
        tx.setMetodo(metodo);
        tx.setFecha(Instant.now());

        if (TOKEN_RECHAZO.equals(token)) {
            tx.setEstado(TransaccionPago.Estado.RECHAZADA);
            tx.setMotivo("Tarjeta rechazada por el emisor");
        } else if (monto.compareTo(limite) > 0) {
            tx.setEstado(TransaccionPago.Estado.RECHAZADA);
            tx.setMotivo("El monto " + monto + " supera el límite autorizado de " + limite);
        } else {
            tx.setEstado(TransaccionPago.Estado.APROBADA);
        }

        try {
            return repositorio.insert(tx);
        } catch (DuplicateKeyException e) {
            return repositorio.findById(pedidoId).orElseThrow();
        }
    }

    /**
     * Compensación: reembolsa solo si el cobro estaba APROBADO (transición atómica
     * APROBADA -> REEMBOLSADA). Si no hubo cobro o ya se reembolsó, no hace nada.
     */
    public Optional<TransaccionPago> reembolsar(String pedidoId) {
        mongo.updateFirst(
                query(where("id").is(pedidoId).and("estado").is(TransaccionPago.Estado.APROBADA)),
                new Update().set("estado", TransaccionPago.Estado.REEMBOLSADA).set("fecha", Instant.now()),
                TransaccionPago.class);
        return repositorio.findById(pedidoId);
    }
}
