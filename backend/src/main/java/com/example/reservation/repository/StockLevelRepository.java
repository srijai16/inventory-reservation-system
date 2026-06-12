package com.example.reservation.repository;

import com.example.reservation.model.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, String> {

    Optional<StockLevel> findByProductIdAndWarehouseId(String productId, String warehouseId);

    @Modifying
    @Query("UPDATE StockLevel s SET s.reservedUnits = s.reservedUnits + :quantity " +
           "WHERE s.product.id = :productId AND s.warehouse.id = :warehouseId " +
           "AND (s.totalUnits - s.reservedUnits) >= :quantity")
    int incrementReservedUnits(String productId, String warehouseId, int quantity);

    @Modifying
    @Query("UPDATE StockLevel s SET s.reservedUnits = s.reservedUnits - :quantity " +
           "WHERE s.product.id = :productId AND s.warehouse.id = :warehouseId")
    int decrementReservedUnits(String productId, String warehouseId, int quantity);
}
