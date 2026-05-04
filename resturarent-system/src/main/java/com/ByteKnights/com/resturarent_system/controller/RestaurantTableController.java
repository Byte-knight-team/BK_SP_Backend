package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateTableStatusRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.TableResponse;
import com.ByteKnights.com.resturarent_system.service.RestaurantTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
/**
 * Exposes REST endpoints for managing restaurant tables.
 */
public class RestaurantTableController {

    private final RestaurantTableService tableService;

    /**
     * Creates a new table in a branch.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> createTable(@Valid @RequestBody CreateTableRequest request) {
        TableResponse response = tableService.createTable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all configured tables.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<TableResponse>> getAllTables() {
        List<TableResponse> tables = tableService.getAllTables();
        return ResponseEntity.ok(tables);
    }

    /**
     * Returns one table by id.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> getTableById(@PathVariable Long id) {
        TableResponse response = tableService.getTableById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates editable table fields like number, capacity, and status.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> updateTable(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTableRequest request) {
        TableResponse response = tableService.updateTable(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates only the table status.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<?> updateTableStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTableStatusRequest request) {
        TableResponse response = tableService.updateTableStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
