package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.ReservationPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationPaymentRepository extends JpaRepository<ReservationPayment, Long> {

    // All payment/refund rows for one reservation (usually 0, 1, or 2 — a payment and/or a refund).
    List<ReservationPayment> findByReservationIdOrderByIdAsc(Long reservationId);
}
