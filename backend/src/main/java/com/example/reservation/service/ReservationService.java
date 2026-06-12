package com.example.reservation.service;

import com.example.reservation.model.Product;
import com.example.reservation.model.Reservation;
import com.example.reservation.model.ReservationStatus;
import com.example.reservation.model.Warehouse;
import com.example.reservation.repository.ProductRepository;
import com.example.reservation.repository.ReservationRepository;
import com.example.reservation.repository.StockLevelRepository;
import com.example.reservation.repository.WarehouseRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLevelRepository stockLevelRepository;
    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    @GetMapping("/test")
public Object test() {
    return reservationRepository.findAll();
}

    public ReservationService(ProductRepository productRepository,
                              WarehouseRepository warehouseRepository,
                              StockLevelRepository stockLevelRepository,
                              ReservationRepository reservationRepository,
                              StringRedisTemplate redisTemplate,
                              TransactionTemplate transactionTemplate) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.reservationRepository = reservationRepository;
        this.redisTemplate = redisTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public Reservation createReservation(String productId, String warehouseId, String warehouseName, String name, int quantity) {
        // Attempt to atomically increment reservedUnits if there is enough totalUnits
        int updatedRows = stockLevelRepository.incrementReservedUnits(productId, warehouseId, quantity);
        if (updatedRows != 1) {
            throw new IllegalArgumentException("NOT_ENOUGH_STOCK");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID().toString());
        reservation.setProduct(product);
        reservation.setWarehouse(warehouse);
        reservation.setName(name);
        reservation.setWarehouseName(warehouseName);
        reservation.setQuantity(quantity);
        reservation.setStatus(ReservationStatus.PENDING);

        LocalDateTime now = LocalDateTime.now();
        reservation.setExpiresAt(now.plusMinutes(10));
        reservation.setCreatedAt(now);
        reservation.setUpdatedAt(now);

        Reservation saved = reservationRepository.save(reservation);

        // Invalidate products cache
        redisTemplate.delete("products:list");

        return saved;
    }

    @Transactional
    public Reservation confirmReservation(String id) {
        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation == null || reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation expired or invalid");
        }

        LocalDateTime now = LocalDateTime.now();
        if (reservation.getExpiresAt().isBefore(now)) {
            // Expired! Release stock and mark as released
            stockLevelRepository.decrementReservedUnits(
                    reservation.getProduct().getId(),
                    reservation.getWarehouse().getId(),
                    reservation.getQuantity()
            );

            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setReleasedAt(now);
            reservation.setUpdatedAt(now);
            Reservation released = reservationRepository.save(reservation);

            redisTemplate.delete("products:list");
            throw new IllegalStateException("Reservation expired");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(now);
        reservation.setUpdatedAt(now);
        Reservation confirmed = reservationRepository.save(reservation);

        redisTemplate.delete("products:list");
        return confirmed;
    }

    @Transactional
    public Reservation releaseReservation(String id) {
        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation == null || reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation not found or cannot release");
        }

        stockLevelRepository.decrementReservedUnits(
                reservation.getProduct().getId(),
                reservation.getWarehouse().getId(),
                reservation.getQuantity()
        );

        LocalDateTime now = LocalDateTime.now();
        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReleasedAt(now);
        reservation.setUpdatedAt(now);
        Reservation released = reservationRepository.save(reservation);

        redisTemplate.delete("products:list");
        return released;
    }

    public int releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findAllByStatusAndExpiresAtLessThan(
                ReservationStatus.PENDING, now
        );

        int releasedCount = 0;
        for (Reservation reservation : expiredReservations) {
            Boolean released = transactionTemplate.execute(status -> {
                Reservation latest = reservationRepository.findById(reservation.getId()).orElse(null);
                if (latest == null || latest.getStatus() != ReservationStatus.PENDING || latest.getExpiresAt().isAfter(now)) {
                    return false;
                }

                stockLevelRepository.decrementReservedUnits(
                        latest.getProduct().getId(),
                        latest.getWarehouse().getId(),
                        latest.getQuantity()
                );

                latest.setStatus(ReservationStatus.RELEASED);
                latest.setReleasedAt(now);
                latest.setUpdatedAt(now);
                reservationRepository.save(latest);
                return true;
            });

            if (Boolean.TRUE.equals(released)) {
                releasedCount++;
            }
        }

        if (releasedCount > 0) {
            redisTemplate.delete("products:list");
        }

        return releasedCount;
    }
}
