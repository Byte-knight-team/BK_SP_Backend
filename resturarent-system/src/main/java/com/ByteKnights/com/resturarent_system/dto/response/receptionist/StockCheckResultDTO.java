package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class StockCheckResultDTO {
    private boolean allSufficient;
    private List<StockItemResult> results;

    @Data
    @AllArgsConstructor
    public static class StockItemResult {
        private String menuItemName;
        private boolean sufficient;
        private String shortage;   // null if sufficient, else e.g. "Beef: need 0.400 kg, have 0.200 kg"
    }
}
