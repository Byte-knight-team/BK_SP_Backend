package com.ByteKnights.com.resturarent_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {

    private Long id;
    private Long branchId;
    private String branchName;
    private Long categoryId;
    private String categoryName;
    private String subCategory;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean isAvailable;
    private String status;
    private Integer preparationTime;
    private LocalDateTime createdAt;
}
