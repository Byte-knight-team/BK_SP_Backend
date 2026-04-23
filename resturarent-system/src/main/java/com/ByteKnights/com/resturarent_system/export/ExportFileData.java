package com.ByteKnights.com.resturarent_system.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportFileData {

    private String fileName;
    private String contentType;
    private byte[] data;
}