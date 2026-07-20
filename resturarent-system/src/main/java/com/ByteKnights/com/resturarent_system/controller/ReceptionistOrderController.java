package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.StandardResponse;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.ReceptionistCancelOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.request.receptionist.ReceptionistHoldOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderDetailDTO;
import com.ByteKnights.com.resturarent_system.dto.response.receptionist.ReceptionistOrderSummaryDTO;
import com.ByteKnights.com.resturarent_system.service.ReceptionistOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/receptionist/orders")
@CrossOrigin
@RequiredArgsConstructor
public class ReceptionistOrderController {

    private final ReceptionistOrderService receptionistOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getOrdersByStatus(
            @RequestParam String status,
            Principal principal) {
        List<ReceptionistOrderSummaryDTO> orders =
                receptionistOrderService.getOrdersByStatus(status, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", orders), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_VIEW')")
    public ResponseEntity<StandardResponse> getOrderDetail(
            @PathVariable Long id,
            Principal principal) {
        ReceptionistOrderDetailDTO detail =
                receptionistOrderService.getOrderDetail(id, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Success", detail), HttpStatus.OK);
    }

    @PutMapping("/{id}/send-to-kitchen")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> sendToKitchen(
            @PathVariable Long id,
            Principal principal) {
        receptionistOrderService.sendToKitchen(id, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Order sent to kitchen", null), HttpStatus.OK);
    }

    @PutMapping("/{id}/hold")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> holdOrder(
            @PathVariable Long id,
            @RequestBody ReceptionistHoldOrderRequest request,
            Principal principal) {
        receptionistOrderService.holdOrder(id, request.getHoldReason(), principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Order put on hold", null), HttpStatus.OK);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> cancelOrder(
            @PathVariable Long id,
            @RequestBody ReceptionistCancelOrderRequest request,
            Principal principal) {
        receptionistOrderService.cancelOrder(id, request.getCancelReason(), principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Order cancelled", null), HttpStatus.OK);
    }

    @PutMapping("/{id}/collect-payment")
    @PreAuthorize("hasAuthority('RECEPTIONIST_PAYMENT_COLLECT')")
    public ResponseEntity<StandardResponse> collectPayment(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, java.math.BigDecimal> body,
            Principal principal) {
        java.math.BigDecimal cashReceived = body != null ? body.get("cashReceived") : null;
        receptionistOrderService.collectPayment(id, cashReceived, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Payment collected", null), HttpStatus.OK);
    }

    @PutMapping("/{id}/serve")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> serveOrder(
            @PathVariable Long id,
            Principal principal) {
        receptionistOrderService.serveOrder(id, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Order served", null), HttpStatus.OK);
    }

    @PutMapping("/order-items/{itemId}/serve")
    @PreAuthorize("hasAuthority('RECEPTIONIST_ORDER_UPDATE')")
    public ResponseEntity<StandardResponse> serveOrderItem(
            @PathVariable Long itemId,
            Principal principal) {
        receptionistOrderService.serveOrderItem(itemId, principal.getName());
        return new ResponseEntity<>(new StandardResponse(200, "Item served", null), HttpStatus.OK);
    }
}
