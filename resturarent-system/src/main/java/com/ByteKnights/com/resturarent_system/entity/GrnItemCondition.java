package com.ByteKnights.com.resturarent_system.entity;

public enum GrnItemCondition {
    GOOD,     // Item arrived in acceptable condition — will be restocked
    DAMAGED,  // Item arrived damaged — will NOT be restocked
    REJECTED  // Item refused at backdoor (wrong item, expired, etc.) — will NOT be restocked
}
