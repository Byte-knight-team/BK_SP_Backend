package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewImageResponse {
    private String imageKey;
    private String imageUrl;
    private String fileName;
    private String contentType;
}