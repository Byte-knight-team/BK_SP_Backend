package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewImagePresignResponse {
    private String fileName;
    private String contentType;
    private String objectKey;
    private String uploadUrl;
    private long expiresInSeconds;
}