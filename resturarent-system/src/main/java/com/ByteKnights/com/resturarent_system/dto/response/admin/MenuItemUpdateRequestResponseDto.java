package com.ByteKnights.com.resturarent_system.dto.response.admin;

import com.ByteKnights.com.resturarent_system.entity.MenuItemUpdateRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemUpdateRequestResponseDto {
    private Long id;
    private Long chefId;
    private String chefName;
    private Long menuItemId;
    private String menuItemName;
    private String menuCategory;
    private String menuSubCategory;
    private String menuItemImage;
    private String chefNote;
    private String adminNote;
    private MenuItemUpdateRequestStatus status;
    private LocalDateTime createdAt;
}
