package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateCouponRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateCouponRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateCouponStatusRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.CouponResponse;

import com.ByteKnights.com.resturarent_system.entity.Coupon;

import java.util.List;

public interface CouponService {
    CouponResponse createCoupon(CreateCouponRequest request);

    List<CouponResponse> getAllCoupons();

    CouponResponse getCouponById(Long id);

    CouponResponse updateCoupon(Long id, UpdateCouponRequest request);

    CouponResponse updateCouponStatus(Long id, UpdateCouponStatusRequest request);
}
