package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateCouponRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateCouponRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateCouponStatusRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.CouponResponse;
import com.ByteKnights.com.resturarent_system.entity.Coupon;
import com.ByteKnights.com.resturarent_system.entity.CouponStatus;
import com.ByteKnights.com.resturarent_system.exception.DuplicateResourceException;
import com.ByteKnights.com.resturarent_system.exception.InvalidOperationException;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.CouponRepository;
import com.ByteKnights.com.resturarent_system.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        LocalDateTime now = LocalDateTime.now();
        
        // Strict past date validations with a 1-minute buffer for network latency
        if (request.getStartDate().isBefore(now.minusMinutes(1))) {
            throw new InvalidOperationException("Please enter a valid date");
        }
        if (request.getExpirationDate().isBefore(now.minusMinutes(1))) {
            throw new InvalidOperationException("Please enter a valid date");
        }
        if (request.getExpirationDate().isBefore(request.getStartDate())) {
            throw new InvalidOperationException("Expiration date must be after start date");
        }

        String newCode;
        do {
            newCode = com.ByteKnights.com.resturarent_system.util.CouponGeneratorUtil.generateSecureCode();
        } while (couponRepository.existsByCode(newCode));

        Coupon coupon = Coupon.builder()
                .code(newCode)
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscount(request.getMaxDiscount())
                .startDate(request.getStartDate())
                .endDate(request.getExpirationDate())
                .usageLimit(request.getUsageLimit())
                .status(request.getStartDate().isAfter(LocalDateTime.now()) ? CouponStatus.SCHEDULED : CouponStatus.ACTIVE)
                .usedCount(0)
                .build();

        Coupon saved = couponRepository.save(coupon);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public List<CouponResponse> getAllCoupons() {
        try {
            return couponRepository.findAll().stream()
                    .map(this::updateCouponStatusDynamically)
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            String msg = e.toString();
            if (e.getCause() != null)
                msg += " | Cause: " + e.getCause().toString();
            if (e.getStackTrace().length > 0)
                msg += " | at " + e.getStackTrace()[0].toString();
            CouponResponse errCoupon = CouponResponse.builder()
                    .id(-1L)
                    .code("ERROR")
                    .description(msg)
                    .status(CouponStatus.INACTIVE)
                    .build();
            return java.util.Collections.singletonList(errCoupon);
        }
    }

    @Override
    @Transactional
    public CouponResponse getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));
        coupon = updateCouponStatusDynamically(coupon);
        return mapToResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long id, UpdateCouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));

        coupon = updateCouponStatusDynamically(coupon);
        if (coupon.getStatus() == CouponStatus.EXPIRED && !request.getExpirationDate().isAfter(LocalDateTime.now())) {
            throw new InvalidOperationException("Cannot modify an expired coupon unless extending its expiration date");
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            coupon.setDescription(request.getDescription());
        }
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setEndDate(request.getExpirationDate());
        coupon.setUsageLimit(request.getUsageLimit());
        LocalDateTime now = LocalDateTime.now();
        if (request.getStartDate() != null && coupon.getStatus() == CouponStatus.SCHEDULED) {
            if (request.getStartDate().isBefore(now.minusMinutes(1))) {
                throw new InvalidOperationException("Start date cannot be in the past");
            }
            coupon.setStartDate(request.getStartDate());
        }

        // Re-evaluate status if dates are extended into the future
        if (coupon.getStatus() == CouponStatus.EXPIRED && coupon.getEndDate().isAfter(now)) {
            coupon.setStatus(coupon.getStartDate().isAfter(now) ? CouponStatus.SCHEDULED : CouponStatus.ACTIVE);
        } else if (coupon.getStatus() == CouponStatus.ACTIVE && coupon.getStartDate().isAfter(now)) {
            coupon.setStatus(CouponStatus.SCHEDULED);
        } else if (coupon.getStatus() == CouponStatus.SCHEDULED && !coupon.getStartDate().isAfter(now)) {
            coupon.setStatus(CouponStatus.ACTIVE);
        }

        Coupon updated = couponRepository.save(coupon);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CouponResponse updateCouponStatus(Long id, UpdateCouponStatusRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with id: " + id));

        coupon = updateCouponStatusDynamically(coupon);
        if (coupon.getStatus() == CouponStatus.EXPIRED) {
            throw new InvalidOperationException("Cannot modify status of an expired coupon");
        }

        coupon.setStatus(request.getStatus());

        Coupon updated = couponRepository.save(coupon);
        return mapToResponse(updated);
    }

    private Coupon updateCouponStatusDynamically(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;

        if (coupon.getStatus() == CouponStatus.SCHEDULED && !coupon.getStartDate().isAfter(now)) {
            coupon.setStatus(CouponStatus.ACTIVE);
            changed = true;
        }

        if ((coupon.getStatus() == CouponStatus.ACTIVE || coupon.getStatus() == CouponStatus.INACTIVE) 
                && coupon.getEndDate() != null 
                && coupon.getEndDate().isBefore(now)) {
            coupon.setStatus(CouponStatus.EXPIRED);
            changed = true;
        }

        if (changed) {
            return couponRepository.save(coupon);
        }
        return coupon;
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscount(coupon.getMaxDiscount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .status(coupon.getStatus())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}

