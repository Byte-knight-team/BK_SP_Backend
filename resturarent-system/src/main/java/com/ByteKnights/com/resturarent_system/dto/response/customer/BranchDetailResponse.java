package com.ByteKnights.com.resturarent_system.dto.response.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDetailResponse {
    private String name;
    private String address;
    private String contactNumber;
    private String email;
}
