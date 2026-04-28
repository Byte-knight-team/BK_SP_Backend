package com.ByteKnights.com.resturarent_system.dto.request.kitchen;

import com.ByteKnights.com.resturarent_system.entity.ChefWorkStatus;
import lombok.Data;

@Data
public class UpdateWorkStatusDTO {
    private ChefWorkStatus newStatus; // AVAILABLE, COOKING, or ON_BREAK
}
