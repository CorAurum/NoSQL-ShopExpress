package uy.edu.utu.shopexpress.pedidos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido (colección "pedidos").
 *
 * Modelado:
 *  - items y pago: EMBEBIDOS. Se crean, leen y modifican siempre junto al pedido y
 *    su tamaño está acotado.
 *  - usuario_id y producto_id: REFERENCIAS a datos de otros servicios.
 *  - pasos_saga: bitácora embebida de la SAGA.
 *  - idempotency_key: índice único (sparse) que impide crear dos pedidos para la
 *    misma solicitud aunque Redis no esté disponible.
 */
@Document("pedidos")
public class Pedido {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String idempotencyKey;

    @Indexed
    private String usuarioId;

    private EstadoPedido estado;

    private List<ItemPedido> items = new ArrayList<>();

    private BigDecimal total;

    private InfoPago pago;

    private String motivoFallo;

    private List<PasoSaga> pasosSaga = new ArrayList<>();

    private Instant fechaCreacion;

    private Instant fechaActualizacion;

    @JsonProperty("pedido_id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @JsonIgnore
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public List<ItemPedido> getItems() { return items; }
    public void setItems(List<ItemPedido> items) { this.items = items; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public InfoPago getPago() { return pago; }
    public void setPago(InfoPago pago) { this.pago = pago; }

    public String getMotivoFallo() { return motivoFallo; }
    public void setMotivoFallo(String motivoFallo) { this.motivoFallo = motivoFallo; }

    public List<PasoSaga> getPasosSaga() { return pasosSaga; }
    public void setPasosSaga(List<PasoSaga> pasosSaga) { this.pasosSaga = pasosSaga; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(Instant fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
