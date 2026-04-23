package com.ByteKnights.com.resturarent_system.export.provider;

import com.ByteKnights.com.resturarent_system.export.ExportFormat;
import com.ByteKnights.com.resturarent_system.export.ExportTarget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public interface ExportDataProvider {

    ExportTarget getTarget();

    String getBaseFileName();

    Set<ExportFormat> getSupportedFormats();

    default boolean supports(ExportFormat format) {
        return getSupportedFormats() != null && getSupportedFormats().contains(format);
    }

    List<LinkedHashMap<String, Object>> getCsvRows();

    Object getJsonData();
}