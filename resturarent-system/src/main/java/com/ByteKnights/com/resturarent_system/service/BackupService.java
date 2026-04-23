package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.export.ExportFileData;
import com.ByteKnights.com.resturarent_system.export.ExportTarget;
import com.ByteKnights.com.resturarent_system.export.provider.ExportDataProvider;
import com.ByteKnights.com.resturarent_system.export.writer.JsonExportWriter;
import com.ByteKnights.com.resturarent_system.export.writer.ZipExportWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final List<ExportDataProvider> providers;
    private final JsonExportWriter jsonExportWriter;
    private final ZipExportWriter zipExportWriter;

    public ExportFileData buildMember01Backup() {
        Map<String, byte[]> files = new LinkedHashMap<>();

        addJsonBackupFile(files, ExportTarget.AUDIT_LOGS);
        addJsonBackupFile(files, ExportTarget.STAFF);
        addJsonBackupFile(files, ExportTarget.BRANCHES);
        addJsonBackupFile(files, ExportTarget.SYSTEM_CONFIG);

        byte[] zipBytes = zipExportWriter.write(files);

        return ExportFileData.builder()
                .fileName("member01-backup-" + LocalDate.now() + ".zip")
                .contentType("application/zip")
                .data(zipBytes)
                .build();
    }

    private void addJsonBackupFile(Map<String, byte[]> files, ExportTarget target) {
        ExportDataProvider provider = getProvider(target);
        byte[] jsonBytes = jsonExportWriter.write(provider.getJsonData());
        files.put(provider.getBaseFileName() + ".json", jsonBytes);
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