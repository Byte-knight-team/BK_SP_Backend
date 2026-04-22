package com.ByteKnights.com.resturarent_system.dto.request.superadmin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateOperatingHoursRequest {

    @Valid
    @NotEmpty
    private List<OperatingHourItemRequest> operatingHours;
}