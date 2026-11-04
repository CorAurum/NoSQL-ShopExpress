package uy.edu.utu.shopexpress.catalogo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uy.edu.utu.shopexpress.catalogo.model.Reserva;

public interface ReservaRepository extends MongoRepository<Reserva, String> {
}
