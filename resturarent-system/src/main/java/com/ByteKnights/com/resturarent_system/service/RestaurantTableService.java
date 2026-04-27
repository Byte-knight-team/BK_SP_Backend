package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.UpdateTableStatusRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.TableResponse;

import java.util.List;

/**
 * Contract for table management operations.
 */
public interface RestaurantTableService {

    /**
     * Creates a table under a branch.
     */
    TableResponse createTable(CreateTableRequest request);

    /**
     * Returns table details by id.
     */
    TableResponse getTableById(Long id);

    /**
     * Returns all tables.
     */
    List<TableResponse> getAllTables();

    /**
     * Updates table fields.
     */
    TableResponse updateTable(Long id, UpdateTableRequest request);

    /**
     * Updates only table status.
     */
    TableResponse updateTableStatus(Long id, UpdateTableStatusRequest request);

    /**
     * Deletes a table.
     */
    void deleteTable(Long id);
}
