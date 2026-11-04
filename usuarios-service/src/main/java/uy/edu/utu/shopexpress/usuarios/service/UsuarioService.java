package uy.edu.utu.shopexpress.usuarios.service;

import com.mongodb.client.result.UpdateResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uy.edu.utu.shopexpress.usuarios.cache.CacheUsuarios;
import uy.edu.utu.shopexpress.usuarios.dto.CrearUsuarioRequest;
import uy.edu.utu.shopexpress.usuarios.dto.RegistrarCompraRequest;
import uy.edu.utu.shopexpress.usuarios.dto.UsuarioExportado;
import uy.edu.utu.shopexpress.usuarios.dto.UsuarioResponse;
import uy.edu.utu.shopexpress.usuarios.error.NegocioException;
import uy.edu.utu.shopexpress.usuarios.model.Direccion;
import uy.edu.utu.shopexpress.usuarios.model.ResumenCompra;
import uy.edu.utu.shopexpress.usuarios.model.Usuario;
import uy.edu.utu.shopexpress.usuarios.privacidad.ProteccionDatos;
import uy.edu.utu.shopexpress.usuarios.repository.UsuarioRepository;
import uy.edu.utu.shopexpress.usuarios.util.Ids;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final MongoTemplate mongo;
    private final ProteccionDatos proteccion;
    private final CacheUsuarios cache;

    public UsuarioService(UsuarioRepository repositorio, MongoTemplate mongo,
                          ProteccionDatos proteccion, CacheUsuarios cache) {
        this.repositorio = repositorio;
        this.mongo = mongo;
        this.proteccion = proteccion;
        this.cache = cache;
    }

    public UsuarioResponse crear(CrearUsuarioRequest solicitud) {
        return crearConId(Ids.nuevo("usr"), solicitud);
    }

    /** Usado también por la carga de datos semilla, que necesita IDs fijos. */
    public UsuarioResponse crearConId(String id, CrearUsuarioRequest solicitud) {
        String email = solicitud.email().trim().toLowerCase(Locale.ROOT);
        String tipoDocumento = solicitud.tipoDocumento().trim().toUpperCase(Locale.ROOT);
        String nroDocumento = solicitud.nroDocumento().trim();
        String documentoHash = proteccion.seudonimizar(tipoDocumento + ":" + nroDocumento);

        // Chequeo previo para dar un mensaje claro; el índice único es la garantía real.
        if (repositorio.existsByEmail(email)) {
            throw new NegocioException(HttpStatus.CONFLICT, "EMAIL_DUPLICADO", "Ya existe un usuario con el email " + email);
        }
        if (repositorio.existsByDocumentoHash(documentoHash)) {
            throw new NegocioException(HttpStatus.CONFLICT, "DOCUMENTO_DUPLICADO", "Ya existe un usuario con ese documento");
        }

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNroDocumentoCifrado(proteccion.cifrar(nroDocumento));
        usuario.setDocumentoHash(documentoHash);
        usuario.setNombre(solicitud.nombre().trim());
        usuario.setApellido(solicitud.apellido().trim());
        usuario.setEmail(email);
        usuario.setDirecciones(solicitud.direcciones().stream()
                .map(d -> new Direccion(d.calle().trim(), d.ciudad().trim(), d.codigoPostal().trim()))
                .toList());
        usuario.setFechaAlta(Instant.now());

        try {
            return aRespuesta(repositorio.insert(usuario));
        } catch (DuplicateKeyException e) {
            // Dos altas simultáneas con el mismo email/documento: gana una, la otra recibe 409.
            throw new NegocioException(HttpStatus.CONFLICT, "USUARIO_DUPLICADO", "Ya existe un usuario con ese email o documento");
        }
    }

    /** Lectura cache-aside: Redis primero, MongoDB si no está (o si Redis no responde). */
    public UsuarioResponse obtener(String id) {
        return cache.obtener(id).orElseGet(() -> {
            UsuarioResponse respuesta = aRespuesta(buscar(id));
            cache.guardar(respuesta);
            return respuesta;
        });
    }

    /**
     * Agrega un pedido confirmado al historial. Es IDEMPOTENTE: la condición
     * "historial_compras.pedido_id != X" hace que un reintento no duplique la entrada,
     * y $push es atómico sobre el documento.
     */
    public UsuarioResponse registrarCompra(String id, RegistrarCompraRequest solicitud) {
        ResumenCompra resumen = new ResumenCompra(solicitud.pedidoId(), Instant.now(),
                solicitud.total(), solicitud.cantidadItems());

        UpdateResult resultado = mongo.updateFirst(
                query(where("id").is(id).and("historialCompras.pedidoId").ne(solicitud.pedidoId())),
                new Update().push("historialCompras", resumen),
                Usuario.class);

        if (resultado.getMatchedCount() == 0 && !repositorio.existsById(id)) {
            throw noEncontrado(id);
        }
        cache.invalidar(id);
        return aRespuesta(buscar(id));
    }

    public List<UsuarioExportado> exportarAnonimizado() {
        return repositorio.findAll().stream().map(this::anonimizar).toList();
    }

    private UsuarioExportado anonimizar(Usuario u) {
        List<ResumenCompra> compras = u.getHistorialCompras() == null ? List.of() : u.getHistorialCompras();
        BigDecimal total = compras.stream()
                .map(ResumenCompra::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Direccion> direcciones = u.getDirecciones() == null ? List.of() : u.getDirecciones();

        return new UsuarioExportado(
                proteccion.seudonimizar("usuario:" + u.getId()).substring(0, 16),
                u.getTipoDocumento(),
                u.getDocumentoHash().substring(0, 16),
                ProteccionDatos.enmascararEmail(u.getEmail()),
                direcciones.stream().map(Direccion::ciudad).distinct().toList(),
                direcciones.stream().map(Direccion::codigoPostal).distinct().toList(),
                compras.size(),
                total,
                u.getFechaAlta() == null ? null : YearMonth.from(u.getFechaAlta().atZone(ZoneOffset.UTC)).toString());
    }

    private Usuario buscar(String id) {
        return repositorio.findById(id).orElseThrow(() -> noEncontrado(id));
    }

    private UsuarioResponse aRespuesta(Usuario u) {
        String documento = ProteccionDatos.enmascararDocumento(proteccion.descifrar(u.getNroDocumentoCifrado()));
        return new UsuarioResponse(u.getId(), u.getTipoDocumento(), documento, u.getNombre(), u.getApellido(),
                u.getEmail(), u.getDirecciones(), u.getHistorialCompras(), u.getFechaAlta());
    }

    private static NegocioException noEncontrado(String id) {
        return new NegocioException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO", "No existe el usuario " + id);
    }
}
