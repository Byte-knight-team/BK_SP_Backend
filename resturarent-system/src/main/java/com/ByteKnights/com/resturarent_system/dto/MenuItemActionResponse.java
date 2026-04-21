package com.ByteKnights.com.resturarent_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemActionResponse {

    private String type;
    private Long menuItemId;
    private String menuItemName;
    private String message;
    private LocalDateTime timestamp;
}
