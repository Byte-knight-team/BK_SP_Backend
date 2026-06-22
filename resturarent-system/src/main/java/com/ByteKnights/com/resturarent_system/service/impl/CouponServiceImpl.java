package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateCouponRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.CouponResponse;
import com.ByteKnights.com.resturarent_system.entity.Coupon;
import com.ByteKnights.com.resturarent_system.entity.CouponStatus;
import com.ByteKnights.com.resturarent_system.exception.DuplicateResourceException;
import com.ByteKnights.com.resturarent_system.repository.CouponRepository;
import com.ByteKnights.com.resturarent_system.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new DuplicateResourceException("Coupon code already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscount(request.getMaxDiscount())
                .startDate(request.getStartDate().atStartOfDay())
                .endDate(request.getExpirationDate().atTime(23, 59, 59))
                .usageLimit(request.getUsageLimit())
                .status(CouponStatus.ACTIVE)
                .usedCount(0)
                .build();

        Coupon saved = couponRepository.save(coupon);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
                .build();
    }
}
