package com.ByteKnights.com.resturarent_system.entity;

public enum OrderStatus {
    PLACED,
    CANCELLED,
    APPROVED,
    REJECTED,
    PENDING,
    PREPARING,
    READY,
    OUT_FOR_DELIVERY,
    SERVED,
    COMPLETED,
    ON_HOLD
}

/*
 * PENDING // waiting for cheff to start preparing.
 * PREPARING // cheff is preparing the order
 * ON-HOLD // order is on hold - chef hold the order because there is an issue in
 * kitchen side. then receiptionist can take action on it.cancel or update the
 * order or send back to the kitchen
 * COMPLETED // order is completed - cooking done
 * CLOSED // order is closed - payment done
 * CANCELLED // order is cancelled by receiptionist or user
 */

/*
PLACED // just placed by customer
CANCELLED // order is cancelled by user or receiptionist or auto cacelld cause error
*/
