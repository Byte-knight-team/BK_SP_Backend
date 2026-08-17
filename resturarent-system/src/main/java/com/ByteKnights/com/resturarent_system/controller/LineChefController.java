package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefCookingStatsDTO;
import com.ByteKnights.com.resturarent_system.dto.response.kitchen.LineChefItemDTO;
import com.ByteKnights.com.resturarent_system.service.LineChefService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/line-chef")
@CrossOrigin
@RequiredArgsConstructor
public class LineChefController {

    private final LineChefService lineChefService;

    @GetMapping("/my-items")
    @PreAuthorize("hasAuthority('LINE_CHEF_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getMyItems(Principal principal) {
        List<LineChefItemDTO> items = lineChefService.getMyItems(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", items), HttpStatus.OK);
    }

    @PutMapping("/order-items/{itemId}/start")
    @PreAuthorize("hasAuthority('LINE_CHEF_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> startItem(
            @PathVariable Long itemId,
            Principal principal) {
        lineChefService.startItem(itemId, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Item started", null), HttpStatus.OK);
    }

    @PutMapping("/order-items/{itemId}/complete")
    @PreAuthorize("hasAuthority('LINE_CHEF_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> completeItem(
            @PathVariable Long itemId,
            Principal principal) {
        lineChefService.completeItem(itemId, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Item completed", null), HttpStatus.OK);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('LINE_CHEF_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getCookingHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            Principal principal) {
        var history = lineChefService.getCookingHistory(principal.getName(), page, size, date, status);
        return new ResponseEntity<>(new StandardResponse(200, "Success", history), HttpStatus.OK);
    }

    @GetMapping("/history/stats")
    @PreAuthorize("hasAuthority('LINE_CHEF_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getCookingStats(Principal principal) {
        LineChefCookingStatsDTO stats = lineChefService.getCookingStats(principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", stats), HttpStatus.OK);
    }
}
