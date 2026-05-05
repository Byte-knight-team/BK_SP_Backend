package com.ByteKnights.com.resturarent_system.dto.response.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerStaffSummaryDTO {
    private String branchName;
    private int kitchenCount;
    private int deliveryCount;
    private int receptionistCount;
    private List<ManagerStaffMemberDTO> staffMembers;
}
