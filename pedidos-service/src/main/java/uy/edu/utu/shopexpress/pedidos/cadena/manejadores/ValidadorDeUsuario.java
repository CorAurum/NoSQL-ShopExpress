package uy.edu.utu.shopexpress.pedidos.cadena.manejadores;

import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.pedidos.cadena.ContextoPedido;
import uy.edu.utu.shopexpress.pedidos.cadena.ManejadorPedido;
import uy.edu.utu.shopexpress.pedidos.cliente.UsuariosClient;

/** Eslabón 2: el usuario debe existir (consulta al Servicio de Usuarios). */
@Component
public class ValidadorDeUsuario extends ManejadorPedido {

    private final UsuariosClient usuarios;

    public ValidadorDeUsuario(UsuariosClient usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void manejar(ContextoPedido ctx) {
        ctx.setUsuario(usuarios.obtener(ctx.getSolicitud().usuarioId().trim()));
    }

    @Override
    public String nombre() {
        return "VALIDAR_USUARIO";
    }
}
