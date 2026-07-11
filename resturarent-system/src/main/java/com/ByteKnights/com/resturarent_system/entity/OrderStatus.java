package com.ByteKnights.com.resturarent_system.entity;

public enum OrderStatus {
    PLACED,
    CANCELLED,
    REJECTED,
    PENDING,
    PREPARING,
    READY,
    OUT_FOR_DELIVERY,
    ARRIVED, // when delivery arrive targeted location
    SERVED,
    COMPLETED,
    ON_HOLD,
    REFUNDED,
    APPROVED

}

/*
 * PENDING // waiting for cheff to start preparing
 * PREPARING // cheff is preparing the order
 * ON-HOLD // order is on hold - chef hold the order because there is an issue in
 * kitchen side. then receiptionist can take action on it. (contact the customer and cancel it.)
 * receiptionist can too update order state to ON_HOLD
 * COMPLETED // order is completed - cooking done
 * CANCELLED // order is cancelled by receiptionist or user
 */
/*
 * PLACED // just placed by customer (only qr and online pickup orders handle by the receptionist)
 */
/*
 * SERVED // is the last state of order receiptionist and delivery person should
 * update this to served
 */
