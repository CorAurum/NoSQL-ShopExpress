package uy.edu.utu.shopexpress.pedidos.model;

import java.time.Instant;

/** Entrada de la bitácora de la SAGA guardada dentro del pedido (trazabilidad / auditoría). */
public record PasoSaga(String paso, ResultadoPaso resultado, String detalle, Instant fecha) {
}
