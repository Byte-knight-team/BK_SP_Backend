package com.ByteKnights.com.resturarent_system.export.writer;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ZipExportWriter {

    public byte[] write(Map<String, byte[]> files) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            if (files != null) {
                for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                    String fileName = entry.getKey();
                    byte[] data = entry.getValue() != null ? entry.getValue() : new byte[0];

                    if (fileName == null || fileName.trim().isEmpty()) {
                        continue;
                    }

                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.write(data);
                    zipOutputStream.closeEntry();
                }
            }

            zipOutputStream.finish();
            return byteArrayOutputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to write ZIP export", e);
        }
    }
}