package com.ByteKnights.com.resturarent_system.repository;

import com.ByteKnights.com.resturarent_system.entity.Coupon;
import com.ByteKnights.com.resturarent_system.entity.CouponUsage;
import com.ByteKnights.com.resturarent_system.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCouponAndCustomer(Coupon coupon, Customer customer);
}