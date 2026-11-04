package uy.edu.utu.shopexpress.pedidos.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Registro de la pasarela de pago simulada (colección "transacciones_pago").
 * El _id es el pedido_id: un pedido no puede cobrarse dos veces.
 */
@Document("transacciones_pago")
public class TransaccionPago {

    public enum Estado { APROBADA, RECHAZADA, REEMBOLSADA }

    @Id
    private String id;

    private String transaccionId;

    private BigDecimal monto;

    private String metodo;

    private Estado estado;

    private String motivo;

    private Instant fecha;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTransaccionId() { return transaccionId; }
    public void setTransaccionId(String transaccionId) { this.transaccionId = transaccionId; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
}
