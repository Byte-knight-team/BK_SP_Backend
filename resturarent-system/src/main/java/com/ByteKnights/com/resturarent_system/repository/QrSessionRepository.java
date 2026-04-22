package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Customer;
import com.ByteKnights.com.resturarent_system.entity.QrSession;
import com.ByteKnights.com.resturarent_system.entity.QrSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrSessionRepository extends JpaRepository<QrSession, Long> {
    Optional<QrSession> findByCustomerAndStatus(Customer customer, QrSessionStatus status);
}
