package com.example.reservation.repository;

import com.example.reservation.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.stocks s LEFT JOIN FETCH s.warehouse ORDER BY p.createdAt DESC")
    List<Product> findAllWithStocksOrderByCreatedAtDesc();
}
