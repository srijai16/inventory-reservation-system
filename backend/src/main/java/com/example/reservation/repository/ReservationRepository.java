package com.example.reservation.repository;

import com.example.reservation.model.Reservation;
import com.example.reservation.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
    List<Reservation> findAllByStatusAndExpiresAtLessThan(ReservationStatus status, LocalDateTime expiresAt);
}
