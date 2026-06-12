package com.ByteKnights.com.resturarent_system.dto.response.manager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerSalesSummaryDTO {
    private BigDecimal grossSales;
    private BigDecimal netSales;
    private BigDecimal totalRefunds;
    
    private BigDecimal cardPayments;
    private BigDecimal cashPayments;
    
    private BigDecimal dineInOrders;
    private BigDecimal deliveryOrders;
    
    private List<TransactionDTO> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDTO {
        private String id;
        private String date;
        private String customer;
        private String mode;
        private BigDecimal amount;
        private String status;
    }
}
