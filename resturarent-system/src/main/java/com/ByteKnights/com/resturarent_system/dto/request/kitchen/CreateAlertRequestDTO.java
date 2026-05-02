package com.ByteKnights.com.resturarent_system.dto.request.kitchen;

import com.ByteKnights.com.resturarent_system.entity.KitchenAlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAlertRequestDTO {
    @NotBlank(message = "Message cannot be empty")
    private String message;

    @NotNull(message = "Alert type is required")
    private KitchenAlertType type;
}
