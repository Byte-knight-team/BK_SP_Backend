package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.ReservationPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.ByteKnights.com.resturarent_system.entity.PaymentStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationPaymentRepository extends JpaRepository<ReservationPayment, Long> {

    // All payment/refund rows for one reservation (usually 0, 1, or 2 — a payment
    // and/or a refund).
    List<ReservationPayment> findByReservationIdOrderByIdAsc(Long reservationId);

    Optional<ReservationPayment> findByReservation(
            com.ByteKnights.com.resturarent_system.entity.Reservation reservation);

    Optional<ReservationPayment> findFirstByReservationOrderByIdDesc(
            com.ByteKnights.com.resturarent_system.entity.Reservation reservation);

    @Modifying
    @Transactional
    @Query("UPDATE ReservationPayment rp SET rp.paymentStatus = :status WHERE rp.transactionReference = :txnRef")
    int updatePaymentStatusByTransactionReference(@Param("txnRef") String txnRef, @Param("status") PaymentStatus status);
}
