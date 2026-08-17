package com.ByteKnights.com.resturarent_system.dto.request.admin;

import com.ByteKnights.com.resturarent_system.entity.CouponStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCouponStatusRequest {

    @NotNull(message = "Status is required")
    private CouponStatus status;
}
