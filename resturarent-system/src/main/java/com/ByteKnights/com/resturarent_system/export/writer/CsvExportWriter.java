package com.ByteKnights.com.resturarent_system.export.writer;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class CsvExportWriter {

    public byte[] write(List<LinkedHashMap<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new byte[0];
        }

        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        StringBuilder csv = new StringBuilder();

        appendRow(csv, headers);

        for (LinkedHashMap<String, Object> row : rows) {
            List<String> values = new ArrayList<>();

            for (String header : headers) {
                values.add(escape(row.get(header)));
            }

            csv.append(String.join(",", values)).append("\r\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendRow(StringBuilder csv, List<String> headers) {
        List<String> escapedHeaders = new ArrayList<>();

        for (String header : headers) {
            escapedHeaders.add(escape(header));
        }

        csv.append(String.join(",", escapedHeaders)).append("\r\n");
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);
        boolean mustQuote = text.contains(",")
                || text.contains("\"")
                || text.contains("\n")
                || text.contains("\r");

        String escaped = text.replace("\"", "\"\"");

        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }
}