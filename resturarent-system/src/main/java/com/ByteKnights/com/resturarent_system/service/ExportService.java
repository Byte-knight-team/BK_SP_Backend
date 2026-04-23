package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.export.ExportFileData;
import com.ByteKnights.com.resturarent_system.export.ExportFormat;
import com.ByteKnights.com.resturarent_system.export.ExportTarget;
import com.ByteKnights.com.resturarent_system.export.provider.ExportDataProvider;
import com.ByteKnights.com.resturarent_system.export.writer.CsvExportWriter;
import com.ByteKnights.com.resturarent_system.export.writer.JsonExportWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final List<ExportDataProvider> providers;
    private final CsvExportWriter csvExportWriter;
    private final JsonExportWriter jsonExportWriter;

    public ExportFileData export(ExportTarget target, ExportFormat format) {
        ExportDataProvider provider = getProvider(target);

        if (!provider.supports(format)) {
            throw new IllegalArgumentException(
                    "Format " + format.getQueryValue() + " is not supported for target " + target.getPathValue()
            );
        }

        byte[] fileBytes;

        switch (format) {
            case CSV -> fileBytes = csvExportWriter.write(provider.getCsvRows());
            case JSON -> fileBytes = jsonExportWriter.write(provider.getJsonData());
            default -> throw new IllegalArgumentException(
                    "Unsupported export format for export endpoint: " + format.getQueryValue()
            );
        }

        String fileName = provider.getBaseFileName()
                + "-" + LocalDate.now()
                + format.getFileExtension();

        return ExportFileData.builder()
                .fileName(fileName)
                .contentType(format.getContentType())
                .data(fileBytes)
                .build();
    }

    private ExportDataProvider getProvider(ExportTarget target) {
        return providers.stream()
                .filter(provider -> provider.getTarget() == target)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No export provider registered for target: " + target.getPathValue()
                ));
    }
}