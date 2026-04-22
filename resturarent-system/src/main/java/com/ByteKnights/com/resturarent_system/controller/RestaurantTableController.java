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
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class RestaurantTableController {

    private final RestaurantTableService tableService;

    @PostMapping
    public ResponseEntity<?> createTable(@Valid @RequestBody CreateTableRequest request) {
        TableResponse response = tableService.createTable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TableResponse>> getAllTables() {
        List<TableResponse> tables = tableService.getAllTables();
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTableById(@PathVariable Long id) {
        TableResponse response = tableService.getTableById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTable(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTableRequest request) {
        TableResponse response = tableService.updateTable(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateTableStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTableStatusRequest request) {
        TableResponse response = tableService.updateTableStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTable(@PathVariable Long id) {
        tableService.deleteTable(id);
        return ResponseEntity.ok(Map.of("message", "Table deleted successfully"));
    }
}
