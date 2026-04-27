package com.ByteKnights.com.resturarent_system.dto.response.kitchen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChefAssignDTO {
    private Long staffId; //as a unique identifier
    private String chefName;
    private String workStatus;
}
