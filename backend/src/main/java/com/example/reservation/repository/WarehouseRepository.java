package com.example.reservation.repository;

import com.example.reservation.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
    List<Warehouse> findAllByOrderByCreatedAtDesc();
}
