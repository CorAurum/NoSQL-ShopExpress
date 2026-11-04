package uy.edu.utu.shopexpress.catalogo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import uy.edu.utu.shopexpress.catalogo.model.Producto;

public interface ProductoRepository extends MongoRepository<Producto, String> {

    boolean existsBySku(String sku);
}
