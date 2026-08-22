package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateCouponRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateCouponRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateCouponStatusRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.CouponResponse;
import com.ByteKnights.com.resturarent_system.service.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CREATE_COUPON')")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('VIEW_COUPONS')")
    public ResponseEntity<List<CouponResponse>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @GetMapping("/test")
    public ResponseEntity<?> testAllCoupons() {
        try {
            return ResponseEntity.ok(couponService.getAllCoupons());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.toString() + " \n " + java.util.Arrays.toString(e.getStackTrace()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('VIEW_COUPON')")
    public ResponseEntity<CouponResponse> getCouponById(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.getCouponById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('UPDATE_COUPON')")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('UPDATE_COUPON_STATUS')")
    public ResponseEntity<CouponResponse> updateCouponStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCouponStatusRequest request) {
        return ResponseEntity.ok(couponService.updateCouponStatus(id, request));
    }
}
