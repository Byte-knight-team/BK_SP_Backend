package com.ByteKnights.com.resturarent_system.export;

import java.util.Arrays;

public enum ExportTarget {

    AUDIT_LOGS("audit-logs"),
    STAFF("staff"),
    BRANCHES("branches"),
    SYSTEM_CONFIG("system-config");

    private final String pathValue;

    ExportTarget(String pathValue) {
        this.pathValue = pathValue;
    }

    public String getPathValue() {
        return pathValue;
    }

    public static ExportTarget fromPath(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Export target is required");
        }

        String normalized = value.trim().toLowerCase();

        return Arrays.stream(values())
                .filter(target -> target.pathValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported export target: " + value));
    }
}