package com.ByteKnights.com.resturarent_system.dto.request.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReviewImagePresignRequest {

    @NotEmpty(message = "At least one file must be specified")
    @Size(max = 10, message = "A maximum of 10 files can be uploaded at once")
    @Valid
    private List<ReviewSubmissionRequest.ReviewImageUploadRequest> files;
}