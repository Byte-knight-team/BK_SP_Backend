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
public class BranchResponse {

    private Long id;
    private String name;
    private String address;
    private String contactNumber;
    private String email;
    private String status;

    /*
     * Exact branch location used by map and delivery-related features.
     */
    private Double latitude;
    private Double longitude;

    /*
     * Existing teammate fields are preserved.
     */
    private Integer reservationMinLeadHours;
    private Integer reservationPaymentWindowMinutes;

    private LocalDateTime createdAt;
    private String message;
}