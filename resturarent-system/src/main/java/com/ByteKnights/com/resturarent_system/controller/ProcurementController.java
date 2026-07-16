package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.dto.ApiResponse;
import com.ByteKnights.com.resturarent_system.dto.request.procurement.CreateGrnRequest;
import com.ByteKnights.com.resturarent_system.dto.request.procurement.CreatePurchaseOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.request.procurement.CreateVendorRequest;
import com.ByteKnights.com.resturarent_system.dto.request.procurement.UpdateVendorRequest;
import com.ByteKnights.com.resturarent_system.dto.response.manager.procurement.GoodsReceiptNoteDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.procurement.ProcurementSummaryDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.procurement.PurchaseOrderDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.procurement.VendorDTO;
import com.ByteKnights.com.resturarent_system.security.JwtUserPrincipal;
import com.ByteKnights.com.resturarent_system.service.ProcurementService;
import com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for all Procurement & Vendor Management operations.
 * Base path: /api/manager/procurement
 *
 * Covers:
 *  - Vendor directory (create, update, deactivate, list)
 *  - Purchase Orders (create, cancel, list, get by id)
 *  - Goods Receipt Notes (create, history, get by id)
 *  - Procurement summary (dashboard cards)
 */
@RestController
@RequestMapping("/api/manager/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    // ─────────────────────────────────────────────────────────────────────────
    // VENDOR ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/manager/procurement/vendors?branchId=
     * List all active vendors for a branch. Used for the vendor directory table
     * and for the vendor dropdown when creating a new PO.
     */
    @GetMapping("/vendors")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<List<VendorDTO>>> getVendors(
            @RequestParam Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        List<VendorDTO> vendors = procurementService.getVendorsByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Vendors retrieved successfully", vendors));
    }

    /**
     * POST /api/manager/procurement/vendors
     * Register a new vendor for the manager's branch.
     */
    @PostMapping("/vendors")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<VendorDTO>> createVendor(
            @RequestBody @Valid CreateVendorRequest request,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        VendorDTO vendor = procurementService.createVendor(request, userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vendor created successfully", vendor));
    }

    /**
     * PUT /api/manager/procurement/vendors/{id}
     * Update an existing vendor's details or re-activate a deactivated vendor.
     */
    @PutMapping("/vendors/{id}")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<VendorDTO>> updateVendor(
            @PathVariable Long id,
            @RequestBody @Valid UpdateVendorRequest request,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        VendorDTO vendor = procurementService.updateVendor(id, request, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Vendor updated successfully", vendor));
    }

    /**
     * DELETE /api/manager/procurement/vendors/{id}
     * Soft-delete a vendor (isActive = false). Existing POs are preserved.
     */
    @DeleteMapping("/vendors/{id}")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<Void>> deactivateVendor(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        procurementService.deactivateVendor(id, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Vendor deactivated successfully", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PURCHASE ORDER ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/manager/procurement/purchase-orders?branchId=&status=
     * List all POs for a branch. Optionally filter by status
     * (SUBMITTED, PARTIALLY_RECEIVED, RECEIVED, CANCELLED).
     * Passing no status returns all POs.
     */
    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<List<PurchaseOrderDTO>>> getPurchaseOrders(
            @RequestParam Long branchId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        List<PurchaseOrderDTO> pos = procurementService.getPurchaseOrders(branchId, status);
        return ResponseEntity.ok(ApiResponse.success("Purchase orders retrieved successfully", pos));
    }

    /**
     * GET /api/manager/procurement/purchase-orders/{id}
     * Get a single PO with all its line items.
     */
    @GetMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> getPurchaseOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        PurchaseOrderDTO po = procurementService.getPurchaseOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Purchase order retrieved successfully", po));
    }

    /**
     * GET /api/manager/procurement/branches/{branchId}/po-logs
     * Get the audit log of all PO status changes.
     */
    @GetMapping("/branches/{branchId}/po-logs")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<List<com.ByteKnights.com.resturarent_system.dto.response.procurement.PurchaseOrderLogDTO>>> getPurchaseOrderLogs(
            @PathVariable Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        List<com.ByteKnights.com.resturarent_system.dto.response.procurement.PurchaseOrderLogDTO> logs = procurementService.getPurchaseOrderLogs(branchId);
        return ResponseEntity.ok(ApiResponse.success("PO logs retrieved successfully", logs));
    }

    /**
     * POST /api/manager/procurement/purchase-orders
     * Create a new Purchase Order. Status is automatically set to SUBMITTED.
     * A unique PO number (PO-YYYY-seq) is auto-generated by the service.
     */
    @PostMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<PurchaseOrderDTO>> createPurchaseOrder(
            @RequestBody @Valid CreatePurchaseOrderRequest request,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        PurchaseOrderDTO po = procurementService.createPurchaseOrder(request, userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Purchase order created successfully", po));
    }

    /**
     * PUT /api/manager/procurement/purchase-orders/{id}/cancel
     * Cancel a PO. Only allowed when the PO is in SUBMITTED status.
     */
    @PutMapping("/purchase-orders/{id}/cancel")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<Void>> cancelPurchaseOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        procurementService.cancelPurchaseOrder(id, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Purchase order cancelled successfully", null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GOODS RECEIPT NOTE ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/manager/procurement/grn?branchId=
     * Get the full GRN history for a branch, ordered newest first.
     */
    @GetMapping("/grn")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<List<GoodsReceiptNoteDTO>>> getGrnHistory(
            @RequestParam Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        List<GoodsReceiptNoteDTO> grns = procurementService.getGrnHistory(branchId);
        return ResponseEntity.ok(ApiResponse.success("GRN history retrieved successfully", grns));
    }

    /**
     * GET /api/manager/procurement/grn/{id}
     * Get a single GRN with all its line items.
     */
    @GetMapping("/grn/{id}")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<GoodsReceiptNoteDTO>> getGrnById(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        GoodsReceiptNoteDTO grn = procurementService.getGrnById(id);
        return ResponseEntity.ok(ApiResponse.success("GRN retrieved successfully", grn));
    }

    /**
     * POST /api/manager/procurement/grn
     * Record a goods delivery against a PO. For each GOOD condition line item,
     * the system automatically creates an InventoryTransaction(RESTOCK) and
     * updates InventoryItem.quantity. PO status is auto-updated.
     */
    @PostMapping("/grn")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<GoodsReceiptNoteDTO>> createGrn(
            @RequestBody @Valid CreateGrnRequest request,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        GoodsReceiptNoteDTO grn = procurementService.createGrn(request, userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Goods received successfully. Inventory updated.", grn));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY ENDPOINT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/manager/procurement/summary?branchId=
     * Returns summary card data for the Procurement page:
     * active vendors, pending POs, monthly spend, GRNs this month.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<ProcurementSummaryDTO>> getProcurementSummary(
            @RequestParam Long branchId,
            @AuthenticationPrincipal JwtUserPrincipal userDetails) {

        ProcurementSummaryDTO summary = procurementService.getProcurementSummary(branchId);
        return ResponseEntity.ok(ApiResponse.success("Procurement summary retrieved successfully", summary));
    }

    /**
     * Get pending chef requests (APPROVED status) for the manager's branch
     */
    @GetMapping("/pending-chef-requests")
    @PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<List<ChefRequestDTO>>> getPendingChefRequests(
            @RequestParam Long branchId) {
        
        List<ChefRequestDTO> requests = procurementService.getPendingChefRequests(branchId);
        return ResponseEntity.ok(ApiResponse.success("Pending chef requests retrieved successfully", requests));
    }
}
