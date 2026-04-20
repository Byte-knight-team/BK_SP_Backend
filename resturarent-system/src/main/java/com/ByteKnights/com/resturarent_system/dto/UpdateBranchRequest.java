package com.ByteKnights.com.resturarent_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBranchRequest {
    private String name;
    private String address;
    private String contactNumber;
    private String email;
}