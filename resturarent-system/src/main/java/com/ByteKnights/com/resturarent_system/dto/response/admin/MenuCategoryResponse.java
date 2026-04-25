package com.ByteKnights.com.resturarent_system.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private String message;
}
