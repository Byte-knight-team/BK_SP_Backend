package com.ByteKnights.com.resturarent_system.dto.response.receptionist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lightweight paging envelope returned to the client so it can render page controls
 * without depending on Spring's PageImpl serialization shape.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private int page;          // zero-based page index
    private int size;
    private long totalElements;
    private int totalPages;
}
