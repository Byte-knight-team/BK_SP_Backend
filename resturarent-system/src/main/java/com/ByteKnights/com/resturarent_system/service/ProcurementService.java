package com.ByteKnights.com.resturarent_system.service;

import com.ByteKnights.com.resturarent_system.dto.request.procurement.CreateGrnRequest;
import com.ByteKnights.com.resturarent_system.dto.request.procurement.CreatePurchaseOrderRequest;
import com.ByteKnights.com.resturarent_system.dto.request.procurement.CreateVendorRequest;
import com.ByteKnights.com.resturarent_system.dto.request.procurement.UpdateVendorRequest;
import com.ByteKnights.com.resturarent_system.dto.response.manager.procurement.GoodsReceiptNoteDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.procurement.ProcurementSummaryDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.procurement.PurchaseOrderDTO;
import com.ByteKnights.com.resturarent_system.dto.response.manager.procurement.VendorDTO;

import java.util.List;

public interface ProcurementService {

    // ── Vendor Management ────────────────────────────────────────────────────

    /** Register a new vendor for the manager's branch */
    VendorDTO createVendor(CreateVendorRequest request, Long userId);

    /** Update an existing vendor's details */
    VendorDTO updateVendor(Long vendorId, UpdateVendorRequest request, Long userId);

    /**
     * Soft-delete a vendor by setting isActive = false.
     * Existing POs linked to this vendor are preserved for audit history.
     */
    void deactivateVendor(Long vendorId, Long userId);

    /** Get all active vendors for a branch (used for the vendor directory and PO dropdowns) */
    List<VendorDTO> getVendorsByBranch(Long branchId);

    // ── Purchase Order Management ─────────────────────────────────────────────

    List<com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO> getPendingChefRequests(Long branchId);

    /**
     * Create a new Purchase Order against a vendor.
     * Automatically generates a unique poNumber (PO-{YEAR}-{seq}).
     * Status is set to SUBMITTED immediately.
     */
    PurchaseOrderDTO createPurchaseOrder(CreatePurchaseOrderRequest request, Long userId);

    /** Cancel a PO — only allowed when status is SUBMITTED */
    void cancelPurchaseOrder(Long poId, Long userId);

    /**
     * Get all POs for a branch, optionally filtered by status string.
     * Passing null or blank status returns all POs.
     */
    List<PurchaseOrderDTO> getPurchaseOrders(Long branchId, String status);

    /** Get a single PO by its ID including all line items */
    PurchaseOrderDTO getPurchaseOrderById(Long poId);

    /** Get the audit log of all PO status changes for a branch */
    List<com.ByteKnights.com.resturarent_system.dto.response.procurement.PurchaseOrderLogDTO> getPurchaseOrderLogs(Long branchId);

    // ── Goods Receipt Note (GRN) ──────────────────────────────────────────────

    /**
     * Record a goods delivery against an existing PO.
     * For each line item with condition = GOOD:
     *   - Creates an InventoryTransaction(RESTOCK)
     *   - Updates InventoryItem.quantity
     * Auto-generates discrepancy notes where received qty != ordered qty.
     * Updates PO status to PARTIALLY_RECEIVED or RECEIVED.
     */
    GoodsReceiptNoteDTO createGrn(CreateGrnRequest request, Long userId);

    /** Get GRN history for a branch ordered by most recent first */
    List<GoodsReceiptNoteDTO> getGrnHistory(Long branchId);

    /** Get a single GRN by its ID including all line items */
    GoodsReceiptNoteDTO getGrnById(Long grnId);

    // ── Summary ───────────────────────────────────────────────────────────────

    /** Get summary card data for the top of the Procurement page */
    ProcurementSummaryDTO getProcurementSummary(Long branchId);
}
