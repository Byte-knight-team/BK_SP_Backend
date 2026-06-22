package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfilePicturePresignResponse {
    private String uploadUrl;
    private String objectKey;
}
