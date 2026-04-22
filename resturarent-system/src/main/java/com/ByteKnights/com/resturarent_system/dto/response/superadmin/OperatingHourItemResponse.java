package com.ByteKnights.com.resturarent_system.dto.response.superadmin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperatingHourItemResponse {

    private Long id;
    private String dayOfWeek;
    private boolean isOpen;
    private String openTime;
    private String closeTime;
    private String lastOrderTime;
}