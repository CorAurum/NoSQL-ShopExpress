package uy.edu.utu.shopexpress.catalogo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Reserva de stock asociada a un pedido (colección "reservas").
 *
 * El _id ES el pedido_id: la unicidad de la clave primaria hace que una misma
 * reserva no pueda crearse dos veces, lo que vuelve idempotentes los reintentos
 * del orquestador. Los productos se relacionan por REFERENCIA (producto_id).
 */
@Document("reservas")
public class Reserva {

    @Id
    private String id;

    private List<ItemReserva> items;

    private EstadoReserva estado;

    private String motivo;

    /**
     * true si llegó un pedido de liberación mientras la reserva estaba EN_PROCESO
     * (típico tras un timeout del orquestador). Al terminar de reservar, se libera sola.
     */
    private boolean liberacionPendiente;

    private Instant fechaCreacion;

    private Instant fechaActualizacion;

    public Reserva() {
    }

    public Reserva(String id, List<ItemReserva> items, EstadoReserva estado) {
        this.id = id;
        this.items = items;
        this.estado = estado;
        this.fechaCreacion = Instant.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<ItemReserva> getItems() { return items; }
    public void setItems(List<ItemReserva> items) { this.items = items; }

    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public boolean isLiberacionPendiente() { return liberacionPendiente; }
    public void setLiberacionPendiente(boolean liberacionPendiente) { this.liberacionPendiente = liberacionPendiente; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(Instant fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
