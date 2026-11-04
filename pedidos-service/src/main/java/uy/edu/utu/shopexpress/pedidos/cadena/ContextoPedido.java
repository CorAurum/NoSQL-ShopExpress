package uy.edu.utu.shopexpress.pedidos.cadena;

import uy.edu.utu.shopexpress.pedidos.cliente.CatalogoClient.ProductoRemoto;
import uy.edu.utu.shopexpress.pedidos.cliente.UsuariosClient.UsuarioRemoto;
import uy.edu.utu.shopexpress.pedidos.dto.CrearPedidoRequest;
import uy.edu.utu.shopexpress.pedidos.model.ItemPedido;
import uy.edu.utu.shopexpress.pedidos.model.PasoSaga;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;
import uy.edu.utu.shopexpress.pedidos.model.ResultadoPaso;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado de UNA solicitud mientras recorre la cadena. Los manejadores son singletons
 * sin estado; todo lo que varía por pedido vive aquí.
 */
public class ContextoPedido {

    private final CrearPedidoRequest solicitud;
    private final String idempotencyKey;
    private final String simularFallo;

    private List<CrearPedidoRequest.Item> itemsNormalizados = List.of();
    private UsuarioRemoto usuario;
    private final Map<String, ProductoRemoto> productos = new LinkedHashMap<>();
    private List<ItemPedido> items = List.of();
    private BigDecimal total;
    private Pedido pedido;
    private boolean pedidoPersistido;
    private String pasoEnCurso;

    /** Pila LIFO: las compensaciones se ejecutan en orden inverso a los pasos. */
    private final Deque<ManejadorPedido> compensables = new ArrayDeque<>();
    private final List<PasoSaga> pasos = new ArrayList<>();

    public ContextoPedido(CrearPedidoRequest solicitud, String idempotencyKey, String simularFallo) {
        this.solicitud = solicitud;
        this.idempotencyKey = idempotencyKey;
        this.simularFallo = simularFallo;
    }

    public void registrarCompensable(ManejadorPedido manejador) {
        compensables.push(manejador);
    }

    public void registrarPaso(String paso, ResultadoPaso resultado, String detalle) {
        pasos.add(new PasoSaga(paso, resultado, detalle, Instant.now()));
    }

    public void registrarPasoOk(String paso) {
        registrarPaso(paso, ResultadoPaso.OK, null);
    }

    /** Permite forzar fallos para la demo con la cabecera X-Simular-Fallo. */
    public boolean debeFallarEn(String paso) {
        return simularFallo != null && simularFallo.equalsIgnoreCase(paso);
    }

    public CrearPedidoRequest getSolicitud() { return solicitud; }
    public String getIdempotencyKey() { return idempotencyKey; }

    public List<CrearPedidoRequest.Item> getItemsNormalizados() { return itemsNormalizados; }
    public void setItemsNormalizados(List<CrearPedidoRequest.Item> itemsNormalizados) { this.itemsNormalizados = itemsNormalizados; }

    public UsuarioRemoto getUsuario() { return usuario; }
    public void setUsuario(UsuarioRemoto usuario) { this.usuario = usuario; }

    public Map<String, ProductoRemoto> getProductos() { return productos; }

    public List<ItemPedido> getItems() { return items; }
    public void setItems(List<ItemPedido> items) { this.items = items; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public boolean isPedidoPersistido() { return pedidoPersistido; }
    public void marcarPersistido() { this.pedidoPersistido = true; }

    public String getPasoEnCurso() { return pasoEnCurso; }
    public void setPasoEnCurso(String pasoEnCurso) { this.pasoEnCurso = pasoEnCurso; }

    public Deque<ManejadorPedido> getCompensables() { return compensables; }

    public List<PasoSaga> getPasos() { return pasos; }
}
