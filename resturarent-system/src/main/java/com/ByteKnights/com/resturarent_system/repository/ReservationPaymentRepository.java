package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.ReservationPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationPaymentRepository extends JpaRepository<ReservationPayment, Long> {
}
