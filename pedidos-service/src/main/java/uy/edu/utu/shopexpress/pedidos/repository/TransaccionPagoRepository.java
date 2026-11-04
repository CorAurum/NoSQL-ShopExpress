package uy.edu.utu.shopexpress.pedidos.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uy.edu.utu.shopexpress.pedidos.model.TransaccionPago;

public interface TransaccionPagoRepository extends MongoRepository<TransaccionPago, String> {
}
