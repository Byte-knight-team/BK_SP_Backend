package com.ByteKnights.com.resturarent_system.dto.request.kitchen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemUpdateRequestDto {
    private Long menuItemId;
    private String chefNote;
}
