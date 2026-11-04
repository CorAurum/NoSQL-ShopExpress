package uy.edu.utu.shopexpress.usuarios.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uy.edu.utu.shopexpress.usuarios.dto.CrearUsuarioRequest;
import uy.edu.utu.shopexpress.usuarios.dto.DireccionRequest;
import uy.edu.utu.shopexpress.usuarios.repository.UsuarioRepository;
import uy.edu.utu.shopexpress.usuarios.service.UsuarioService;

import java.util.List;

/** Carga el usuario de ejemplo de la letra (usr_12345) para facilitar la demo. */
@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true")
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final UsuarioRepository repositorio;
    private final UsuarioService servicio;

    public DatosIniciales(UsuarioRepository repositorio, UsuarioService servicio) {
        this.repositorio = repositorio;
        this.servicio = servicio;
    }

    @Override
    public void run(String... args) {
        if (repositorio.existsById("usr_12345")) {
            return;
        }
        servicio.crearConId("usr_12345", new CrearUsuarioRequest(
                "DNI", "12345678", "Juan", "Pérez", "juan.perez@example.com",
                List.of(new DireccionRequest("Av. Principal 123", "Montevideo", "11000"))));
        log.info("Usuario semilla usr_12345 creado");
    }
}
