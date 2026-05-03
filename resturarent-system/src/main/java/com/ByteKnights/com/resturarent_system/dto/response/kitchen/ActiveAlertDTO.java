package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import com.ByteKnights.com.resturarent_system.entity.KitchenAlertType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveAlertDTO {
    private Long id;
    private String message;
    private KitchenAlertType type;
    private String timeAgo; // ex: "2m", "1h"
}
