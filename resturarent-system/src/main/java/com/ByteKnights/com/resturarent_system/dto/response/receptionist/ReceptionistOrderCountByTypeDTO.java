package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceptionistOrderCountByTypeDTO {
    private long qrCount;
    private long pickupCount;
    private long deliveryCount;
}
