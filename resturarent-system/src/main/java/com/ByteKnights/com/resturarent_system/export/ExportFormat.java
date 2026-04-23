package com.ByteKnights.com.resturarent_system.export;

import java.util.Arrays;

public enum ExportFormat {

    CSV("csv", "text/csv", ".csv"),
    JSON("json", "application/json", ".json"),
    ZIP("zip", "application/zip", ".zip");

    private final String queryValue;
    private final String contentType;
    private final String fileExtension;

    ExportFormat(String queryValue, String contentType, String fileExtension) {
        this.queryValue = queryValue;
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public static ExportFormat fromQuery(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Export format is required");
        }

        String normalized = value.trim().toLowerCase();

        return Arrays.stream(values())
                .filter(format -> format.queryValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported export format: " + value));
    }
}