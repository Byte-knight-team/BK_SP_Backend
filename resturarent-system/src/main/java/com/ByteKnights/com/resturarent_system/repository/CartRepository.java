package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Cart;
import com.ByteKnights.com.resturarent_system.entity.CartStatus;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomerAndStatus(Customer customer, CartStatus status);

    List<Cart> findByCustomerOrderByUpdatedAtDesc(Customer customer);
}