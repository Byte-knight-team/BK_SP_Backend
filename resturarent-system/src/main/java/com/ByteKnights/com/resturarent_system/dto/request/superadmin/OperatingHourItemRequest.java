package com.ByteKnights.com.resturarent_system.dto.request.superadmin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperatingHourItemRequest {

    @NotBlank
    private String dayOfWeek;

    private Boolean isOpen;

    private String openTime;
    private String closeTime;
    private String lastOrderTime;
}