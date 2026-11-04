package uy.edu.utu.shopexpress.usuarios.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uy.edu.utu.shopexpress.usuarios.model.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    boolean existsByEmail(String email);

    boolean existsByDocumentoHash(String documentoHash);
}
