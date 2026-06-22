package com.ByteKnights.com.resturarent_system.dto.request.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfilePictureUpdateRequest {
    @NotBlank(message = "Object key is required")
    private String objectKey;
}
