package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for handling all HTTP requests related to Inventory
 * Management.
 * 
 * The @RestController annotation tells Spring that this class is an API
 * controller,
 * and it will automatically convert all returned Java objects directly into
 * JSON
 * for the frontend to consume.
 * 
 * The @RequestMapping("/api/inventory") annotation means that every endpoint we
 * build
 * inside this class will automatically start with
 * "http://localhost:8080/api/inventory".
 */

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    /**
     * INJECTED SERVICE
     * 
     * We inject the InventoryService INTERFACE here, not the implementation class.
     * This ensures our controller is completely decoupled from database logic.
     * The controller's only job is to receive the HTTP request, pass the data to
     * the
     * service, and return the service's response back to the frontend.
     */
    private final InventoryService inventoryService;

}
