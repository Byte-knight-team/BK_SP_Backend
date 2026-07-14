package com.ByteKnights.com.resturarent_system.dto.request.admin;

import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemUpdateDecisionDto {
    private MenuItemUpdateRequestStatus status;
    private String adminNote;
}
