package com.ByteKnights.com.resturarent_system.dto.response.manager.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCategoryDTO {
    private String category;
    private BigDecimal value;
    private Long itemCount;
}
