package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateCouponRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.CouponResponse;

import java.util.List;

public interface CouponService {
    CouponResponse createCoupon(CreateCouponRequest request);
    List<CouponResponse> getAllCoupons();
}
