package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.UpdateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.UpdateTableStatusRequest;
import com.ByteKnights.com.resturarent_system.dto.request.admin.CreateTableRequest;
import com.ByteKnights.com.resturarent_system.dto.response.admin.TableResponse;

import java.util.List;

public interface RestaurantTableService {

    TableResponse createTable(CreateTableRequest request);

    TableResponse getTableById(Long id);

    List<TableResponse> getAllTables();

    TableResponse updateTable(Long id, UpdateTableRequest request);

    TableResponse updateTableStatus(Long id, UpdateTableStatusRequest request);

    void deleteTable(Long id);
}
