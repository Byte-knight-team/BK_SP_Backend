package com.ByteKnights.com.resturarent_system.entity;

public enum ChefWorkStatus {
    AVAILABLE, // Chef is clocked in and ready to start preparing meals
    COOKING, // Chef is currently at their station preparing at least one meal.
    ON_BREAK, // Chef is clocked in but temporarily away (e.g., meal break or emergency)
    UNAVAILABLE // Chef is clocked out or has not started their shift for the day yet.
}
