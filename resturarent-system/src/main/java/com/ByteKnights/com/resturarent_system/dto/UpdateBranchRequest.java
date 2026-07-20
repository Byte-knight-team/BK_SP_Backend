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

    /*
     * Exact map-selected branch coordinates.
     *
     * These remain optional in an update request so older requests that
     * update only basic branch information do not remove existing coordinates.
     */
    private Double latitude;
    private Double longitude;
}