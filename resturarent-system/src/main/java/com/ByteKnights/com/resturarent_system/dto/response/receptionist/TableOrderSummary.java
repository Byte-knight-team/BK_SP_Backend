package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A compact summary of one order sitting on a table — lets the receptionist see, per
 * table, what's still cooking/ready and how much is due before the table can be cleared.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableOrderSummary {

    private String orderNumber;
    private String paymentStatus;
    private String orderStatus;      // to detect SERVED vs still preparing
    private int readyItemCount;       // items cooked & waiting to be served
    private double finalAmount;       // bill amount, for "Rs. X due"
}
