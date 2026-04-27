package com.ByteKnights.com.resturarent_system.dto.request.inventory;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveChefRequestDTO {

    @NotNull(message = "Status is required")
    private String status;

    private String managerNote;
}
