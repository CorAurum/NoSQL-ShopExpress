package uy.edu.utu.shopexpress.pedidos.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uy.edu.utu.shopexpress.pedidos.model.Pedido;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends MongoRepository<Pedido, String> {

    Optional<Pedido> findByIdempotencyKey(String idempotencyKey);

    List<Pedido> findByUsuarioIdOrderByFechaCreacionDesc(String usuarioId);
}
