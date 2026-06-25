package com.ByteKnights.com.resturarent_system.dto.request.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfilePicturePresignRequest {
    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Content type is required")
    private String contentType;
}
