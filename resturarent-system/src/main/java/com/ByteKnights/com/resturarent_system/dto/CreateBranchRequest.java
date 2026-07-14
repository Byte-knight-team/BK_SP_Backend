package com.ByteKnights.com.resturarent_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBranchRequest {

    private String name;
    private String address;
    private String contactNumber;
    private String email;

    /*
     * Exact map-selected branch coordinates.
     * The frontend obtains these through LocationPickerModal.
     */
    private Double latitude;
    private Double longitude;
}