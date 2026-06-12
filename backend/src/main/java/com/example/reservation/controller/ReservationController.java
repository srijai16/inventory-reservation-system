package com.example.reservation.controller;

import com.example.reservation.dto.ReservationRequest;
import com.example.reservation.dto.ReservationResponse;
import com.example.reservation.model.Reservation;
import com.example.reservation.repository.ReservationRepository;
import com.example.reservation.service.IdempotencyService;
import com.example.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final IdempotencyService idempotencyService;

    public ReservationController(ReservationService reservationService,
                                 ReservationRepository reservationRepository,
                                 IdempotencyService idempotencyService) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<?> createReservation(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReservationRequest req) {

        String endpoint = "POST /api/reservations";
        String requestHash = idempotencyService.hashBody(req);

        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            IdempotencyService.CachedResponse cached = idempotencyService.getCachedResponse(idempotencyKey, endpoint);
            if (cached != null) {
                if (!requestHash.equals(cached.getRequestHash())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Idempotency-Key reused with different request body"));
                }
                return ResponseEntity.status(cached.getStatusCode()).body(cached.getResponseBody());
            }
        }

        try {
            Reservation reservation = reservationService.createReservation(
                    req.getProductId(),
                    req.getWarehouseId(),
                    req.getWarehouseName(),
                    req.getName(),
                    req.getQuantity()
            );

            ReservationResponse responseBody = ReservationResponse.fromEntity(reservation);

            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                idempotencyService.saveResponse(idempotencyKey, endpoint, requestHash, 201, responseBody);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        } catch (IllegalArgumentException e) {
            if ("NOT_ENOUGH_STOCK".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Not enough stock available"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReservation(@PathVariable("id") String id) {
        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Reservation not found"));
        }
        return ResponseEntity.ok(ReservationResponse.fromEntity(reservation));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirmReservation(@PathVariable("id") String id) {
        try {
            Reservation confirmed = reservationService.confirmReservation(id);
            return ResponseEntity.ok(ReservationResponse.fromEntity(confirmed));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal error"));
        }
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<?> releaseReservation(@PathVariable("id") String id) {
        try {
            Reservation released = reservationService.releaseReservation(id);
            return ResponseEntity.ok(ReservationResponse.fromEntity(released));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal error"));
        }
    }
}
