package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.export.ExportFileData;
import com.ByteKnights.com.resturarent_system.export.ExportFormat;
import com.ByteKnights.com.resturarent_system.export.ExportTarget;
import com.ByteKnights.com.resturarent_system.service.BackupService;
import com.ByteKnights.com.resturarent_system.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final BackupService backupService;

    @GetMapping("/exports/{target}")
    public ResponseEntity<byte[]> export(
            @PathVariable String target,
            @RequestParam String format
    ) {
        ExportTarget exportTarget = ExportTarget.fromPath(target);
        ExportFormat exportFormat = ExportFormat.fromQuery(format);

        ExportFileData fileData = exportService.export(exportTarget, exportFormat);
        return buildFileResponse(fileData);
    }

    @GetMapping("/backups/member01")
    public ResponseEntity<byte[]> downloadMember01Backup() {
        ExportFileData fileData = backupService.buildMember01Backup();
        return buildFileResponse(fileData);
    }

    private ResponseEntity<byte[]> buildFileResponse(ExportFileData fileData) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        if (fileData.getContentType() != null && !fileData.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(fileData.getContentType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileData.getFileName() + "\""
                )
                .body(fileData.getData());
    }
}