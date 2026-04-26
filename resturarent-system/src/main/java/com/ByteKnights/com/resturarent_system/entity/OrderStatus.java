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
    ON_HOLD
}

/*
 * PENDING // waiting for cheff to start preparing
 * PREPARING // cheff is preparing the order
 * ON-HOLD // order is on hold - chef hold the order because there is a issue in
 * kitchen side. then receiptionist can take action on it.cancel or update the
 * receiptionist can too update order state to ON_HOLD
 * order or send back to the kitchen
 * COMPLETED // order is completed - cooking done
 * CANCELLED // order is cancelled by receiptionist or user
 */

/*
PLACED // just placed by customer
CANCELLED // order is cancelled by user,receiptionist or auto cacelld cause error
*/

/*
SERVED // is the last state of order receiptionist and delivery person should update this to served
*/