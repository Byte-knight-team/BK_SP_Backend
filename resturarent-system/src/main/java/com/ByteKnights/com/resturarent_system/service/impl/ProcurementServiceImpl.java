package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.dto.request.procurement.*;
import com.ByteKnights.com.resturarent_system.dto.response.manager.procurement.*;
import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.exception.ResourceNotFoundException;
import com.ByteKnights.com.resturarent_system.repository.GoodsReceiptNoteRepository;
import com.ByteKnights.com.resturarent_system.repository.GrnLineItemRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryItemRepository;
import com.ByteKnights.com.resturarent_system.repository.InventoryTransactionRepository;
import com.ByteKnights.com.resturarent_system.repository.PurchaseOrderItemRepository;
import com.ByteKnights.com.resturarent_system.repository.PurchaseOrderRepository;
import com.ByteKnights.com.resturarent_system.repository.StaffRepository;
import com.ByteKnights.com.resturarent_system.repository.VendorRepository;
import com.ByteKnights.com.resturarent_system.repository.ChefRequestRepository;
import com.ByteKnights.com.resturarent_system.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcurementServiceImpl implements ProcurementService {

    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final GrnLineItemRepository grnLineItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final StaffRepository staffRepository;
    private final ChefRequestRepository chefRequestRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Resolves the manager's branch from their userId */
    private Staff resolveStaff(Long userId) {
        return staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user: " + userId));
    }

    /**
     * Auto-generates a unique PO number in the format PO-{YEAR}-{3-digit seq}.
     * e.g. PO-2026-001, PO-2026-002, ...
     * Looks at the highest existing PO number for this branch/year and increments by 1.
     */
    private String generatePoNumber(Long branchId) {
        int year = Year.now().getValue();
        String yearPrefix = "PO-" + year + "-";
        String maxPoNumber = purchaseOrderRepository.findMaxPoNumberByBranchAndYear(branchId, yearPrefix);

        int nextSeq = 1;
        if (maxPoNumber != null) {
            try {
                String seqPart = maxPoNumber.substring(yearPrefix.length());
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException | IndexOutOfBoundsException ignored) {
                nextSeq = 1;
            }
        }
        return yearPrefix + String.format("%03d", nextSeq);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VENDOR MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public VendorDTO createVendor(CreateVendorRequest request, Long userId) {
        Staff staff = resolveStaff(userId);

        Vendor vendor = Vendor.builder()
                .branch(staff.getBranch())
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .category(request.getCategory())
                .isActive(true)
                .build();

        return toVendorDTO(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    public VendorDTO updateVendor(Long vendorId, UpdateVendorRequest request, Long userId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));

        vendor.setName(request.getName());
        vendor.setContactPerson(request.getContactPerson());
        vendor.setPhone(request.getPhone());
        vendor.setEmail(request.getEmail());
        vendor.setAddress(request.getAddress());
        vendor.setCategory(request.getCategory());
        vendor.setActive(request.isActive());

        return toVendorDTO(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    public void deactivateVendor(Long vendorId, Long userId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));
        vendor.setActive(false);
        vendorRepository.save(vendor);
    }

    @Override
    public List<VendorDTO> getVendorsByBranch(Long branchId) {
        return vendorRepository.findByBranchIdAndIsActiveTrueOrderByNameAsc(branchId)
                .stream()
                .map(this::toVendorDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHEF REQUESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO> getPendingChefRequests(Long branchId) {
        return chefRequestRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, ChefRequestStatus.APPROVED)
                .stream()
                .map(this::toChefRequestDTO)
                .collect(Collectors.toList());
    }

    private com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO toChefRequestDTO(ChefRequest req) {
        java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = req.getCreatedAt() != null ? req.getCreatedAt().format(timeFormatter) : "";
        String formattedQuantity = req.getRequestedQuantity() + " " + req.getUnit();

        String[] colors = { "#F97316", "#3B82F6", "#10B981", "#8B5CF6", "#EF4444", "#EC4899" };
        String color = colors[0];
        if (req.getChefName() != null && !req.getChefName().isEmpty()) {
            color = colors[Math.abs(req.getChefName().hashCode()) % colors.length];
        }

        return com.ByteKnights.com.resturarent_system.dto.response.inventory.ChefRequestDTO.builder()
                .id(req.getId())
                .chefName(req.getChefName())
                .time(formattedTime)
                .item(req.getItemName())
                .quantity(formattedQuantity)
                .note(req.getChefNote())
                .managerNote(req.getManagerNote())
                .status(req.getStatus() != null ? req.getStatus().name() : "PENDING")
                .requestType(req.getRequestType() != null ? req.getRequestType().name() : null)
                .avatarColor(color)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PURCHASE ORDER MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PurchaseOrderDTO createPurchaseOrder(CreatePurchaseOrderRequest request, Long userId) {
        Staff staff = resolveStaff(userId);

        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + request.getVendorId()));

        String poNumber = generatePoNumber(staff.getBranch().getId());

        PurchaseOrder po = PurchaseOrder.builder()
                .branch(staff.getBranch())
                .vendor(vendor)
                .poNumber(poNumber)
                .status(PurchaseOrderStatus.SUBMITTED)
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .notes(request.getNotes())
                .createdBy(staff)
                .build();

        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        if (request.getChefRequestId() != null) {
            ChefRequest chefRequest = chefRequestRepository.findById(request.getChefRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chef request not found with id: " + request.getChefRequestId()));
            if (!chefRequest.getBranch().getId().equals(staff.getBranch().getId())) {
                throw new RuntimeException("Chef request does not belong to your branch");
            }
            chefRequest.setStatus(ChefRequestStatus.ORDERED);
            chefRequestRepository.save(chefRequest);
        }

        // Save each line item
        for (POLineItemRequest lineReq : request.getItems()) {
            InventoryItem inventoryItem = null;
            if (lineReq.getInventoryItemId() != null) {
                inventoryItem = inventoryItemRepository.findById(lineReq.getInventoryItemId())
                        .orElse(null);
            }

            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                    .purchaseOrder(savedPo)
                    .inventoryItem(inventoryItem)
                    .itemNameSnapshot(lineReq.getItemName())
                    .orderedQuantity(lineReq.getOrderedQuantity())
                    .unit(lineReq.getUnit())
                    .agreedUnitPrice(lineReq.getAgreedUnitPrice())
                    .build();

            purchaseOrderItemRepository.save(poItem);
        }

        return getPurchaseOrderById(savedPo.getId());
    }

    @Override
    @Transactional
    public void cancelPurchaseOrder(Long poId, Long userId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + poId));

        if (po.getStatus() != PurchaseOrderStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED purchase orders can be cancelled.");
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(po);
    }

    @Override
    public List<PurchaseOrderDTO> getPurchaseOrders(Long branchId, String status) {
        List<PurchaseOrder> pos;

        if (status != null && !status.isBlank()) {
            try {
                PurchaseOrderStatus statusEnum = PurchaseOrderStatus.valueOf(status.toUpperCase());
                pos = purchaseOrderRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, statusEnum);
            } catch (IllegalArgumentException e) {
                pos = purchaseOrderRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
            }
        } else {
            pos = purchaseOrderRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
        }

        return pos.stream().map(this::toPurchaseOrderDTO).collect(Collectors.toList());
    }

    @Override
    public PurchaseOrderDTO getPurchaseOrderById(Long poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + poId));
        return toPurchaseOrderDTO(po);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GOODS RECEIPT NOTE (GRN)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GoodsReceiptNoteDTO createGrn(CreateGrnRequest request, Long userId) {
        Staff staff = resolveStaff(userId);

        PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + request.getPurchaseOrderId()));

        GoodsReceiptNote grn = GoodsReceiptNote.builder()
                .purchaseOrder(po)
                .invoiceReference(request.getInvoiceReference())
                .receivedBy(staff)
                .notes(request.getNotes())
                .build();

        GoodsReceiptNote savedGrn = goodsReceiptNoteRepository.save(grn);

        // Process each line item
        for (GrnLineItemRequest lineReq : request.getItems()) {
            PurchaseOrderItem poItem = purchaseOrderItemRepository.findById(lineReq.getPurchaseOrderItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("PO line item not found: " + lineReq.getPurchaseOrderItemId()));

            GrnItemCondition condition = lineReq.getCondition() != null
                    ? lineReq.getCondition()
                    : GrnItemCondition.GOOD;

            // Auto-generate discrepancy note if quantities differ
            String discrepancyNote = lineReq.getDiscrepancyNote();
            boolean hasDiscrepancy = lineReq.getReceivedQuantity().compareTo(poItem.getOrderedQuantity()) != 0
                    || condition != GrnItemCondition.GOOD;

            if (hasDiscrepancy && (discrepancyNote == null || discrepancyNote.isBlank())) {
                if (condition != GrnItemCondition.GOOD) {
                    discrepancyNote = "Item received in " + condition.name() + " condition — not added to inventory.";
                } else {
                    BigDecimal diff = poItem.getOrderedQuantity().subtract(lineReq.getReceivedQuantity());
                    discrepancyNote = "Ordered " + poItem.getOrderedQuantity() + " " + poItem.getUnit()
                            + ", received " + lineReq.getReceivedQuantity() + " " + poItem.getUnit()
                            + " — " + diff.abs() + " " + poItem.getUnit() + " short.";
                }
            }

            GrnLineItem lineItem = GrnLineItem.builder()
                    .goodsReceiptNote(savedGrn)
                    .purchaseOrderItem(poItem)
                    .receivedQuantity(lineReq.getReceivedQuantity())
                    .condition(condition)
                    .discrepancyNote(discrepancyNote)
                    .build();

            grnLineItemRepository.save(lineItem);

            // Only restock inventory for GOOD condition items with quantity > 0
            if (condition == GrnItemCondition.GOOD
                    && lineReq.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0
                    && poItem.getInventoryItem() != null) {

                InventoryItem invItem = poItem.getInventoryItem();
                BigDecimal previousQty = invItem.getQuantity() != null ? invItem.getQuantity() : BigDecimal.ZERO;
                BigDecimal newQty = previousQty.add(lineReq.getReceivedQuantity());

                invItem.setQuantity(newQty);
                inventoryItemRepository.save(invItem);

                // Record an InventoryTransaction(RESTOCK) for full audit trail
                InventoryTransaction transaction = InventoryTransaction.builder()
                        .inventoryItem(invItem)
                        .staff(staff)
                        .transactionType(InventoryTransactionType.RESTOCK)
                        .quantityChange(lineReq.getReceivedQuantity())
                        .previousQuantity(previousQty)
                        .newQuantity(newQty)
                        .unitPrice(poItem.getAgreedUnitPrice())
                        .notes("Auto-restocked via GRN for PO: " + po.getPoNumber())
                        .build();

                inventoryTransactionRepository.save(transaction);
            }
        }

        // Update PO status based on total received quantities across ALL GRNs
        updatePoStatusAfterGrn(po);

        return getGrnById(savedGrn.getId());
    }

    /**
     * After saving a GRN, re-evaluates all PO line items to determine if the PO
     * is fully received, partially received, or still submitted.
     */
    private void updatePoStatusAfterGrn(PurchaseOrder po) {
        List<PurchaseOrderItem> poItems = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId());

        boolean allFullyReceived = poItems.stream().allMatch(item -> {
            BigDecimal totalReceived = grnLineItemRepository.sumReceivedQuantityByPoItemId(item.getId());
            totalReceived = totalReceived != null ? totalReceived : BigDecimal.ZERO;
            return totalReceived.compareTo(item.getOrderedQuantity()) >= 0;
        });

        boolean anyReceived = poItems.stream().anyMatch(item -> {
            BigDecimal totalReceived = grnLineItemRepository.sumReceivedQuantityByPoItemId(item.getId());
            totalReceived = totalReceived != null ? totalReceived : BigDecimal.ZERO;
            return totalReceived.compareTo(BigDecimal.ZERO) > 0;
        });

        if (allFullyReceived) {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
        } else if (anyReceived) {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        purchaseOrderRepository.save(po);
    }

    @Override
    public List<GoodsReceiptNoteDTO> getGrnHistory(Long branchId) {
        return goodsReceiptNoteRepository.findByPurchaseOrderBranchIdOrderByReceivedAtDesc(branchId)
                .stream()
                .map(this::toGrnDTO)
                .collect(Collectors.toList());
    }

    @Override
    public GoodsReceiptNoteDTO getGrnById(Long grnId) {
        GoodsReceiptNote grn = goodsReceiptNoteRepository.findById(grnId)
                .orElseThrow(() -> new ResourceNotFoundException("GRN not found with id: " + grnId));
        return toGrnDTO(grn);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ProcurementSummaryDTO getProcurementSummary(Long branchId) {
        long totalActiveVendors = vendorRepository.countByBranchIdAndIsActiveTrue(branchId);
        long activePendingPos = purchaseOrderRepository.countActivePendingByBranchId(branchId);

        // Calculate total spend this month from confirmed GRN line items (GOOD condition)
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<GoodsReceiptNote> monthGrns = goodsReceiptNoteRepository
                .findByPurchaseOrderBranchIdOrderByReceivedAtDesc(branchId)
                .stream()
                .filter(grn -> grn.getReceivedAt() != null && grn.getReceivedAt().isAfter(startOfMonth))
                .collect(Collectors.toList());

        BigDecimal totalMonthlySpend = BigDecimal.ZERO;
        for (GoodsReceiptNote grn : monthGrns) {
            List<GrnLineItem> lineItems = grnLineItemRepository.findByGoodsReceiptNoteId(grn.getId());
            for (GrnLineItem li : lineItems) {
                if (li.getCondition() == GrnItemCondition.GOOD
                        && li.getPurchaseOrderItem().getAgreedUnitPrice() != null) {
                    BigDecimal lineTotal = li.getReceivedQuantity()
                            .multiply(li.getPurchaseOrderItem().getAgreedUnitPrice());
                    totalMonthlySpend = totalMonthlySpend.add(lineTotal);
                }
            }
        }

        return ProcurementSummaryDTO.builder()
                .totalActiveVendors(totalActiveVendors)
                .activePendingPos(activePendingPos)
                .totalMonthlySpend(totalMonthlySpend)
                .totalGrnsThisMonth(monthGrns.size())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPERS (Entity → DTO)
    // ─────────────────────────────────────────────────────────────────────────

    private VendorDTO toVendorDTO(Vendor vendor) {
        long activePoCount = purchaseOrderRepository.countByBranchIdAndStatus(
                vendor.getBranch().getId(), PurchaseOrderStatus.SUBMITTED)
                + purchaseOrderRepository.countByBranchIdAndStatus(
                vendor.getBranch().getId(), PurchaseOrderStatus.PARTIALLY_RECEIVED);

        return VendorDTO.builder()
                .id(vendor.getId())
                .name(vendor.getName())
                .contactPerson(vendor.getContactPerson())
                .phone(vendor.getPhone())
                .email(vendor.getEmail())
                .address(vendor.getAddress())
                .category(vendor.getCategory())
                .isActive(vendor.isActive())
                .activePoCount(activePoCount)
                .build();
    }

    private PurchaseOrderDTO toPurchaseOrderDTO(PurchaseOrder po) {
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId());

        List<PurchaseOrderItemDTO> itemDTOs = items.stream().map(item -> {
            BigDecimal totalReceived = grnLineItemRepository.sumReceivedQuantityByPoItemId(item.getId());

            return PurchaseOrderItemDTO.builder()
                    .id(item.getId())
                    .inventoryItemId(item.getInventoryItem() != null ? item.getInventoryItem().getId() : null)
                    .itemNameSnapshot(item.getItemNameSnapshot())
                    .orderedQuantity(item.getOrderedQuantity())
                    .unit(item.getUnit())
                    .agreedUnitPrice(item.getAgreedUnitPrice())
                    .linkedToCatalog(item.getInventoryItem() != null)
                    .totalReceivedQuantity(totalReceived != null ? totalReceived : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());

        // Calculate total value of PO
        BigDecimal totalValue = items.stream()
                .filter(i -> i.getAgreedUnitPrice() != null)
                .map(i -> i.getOrderedQuantity().multiply(i.getAgreedUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String createdByName = po.getCreatedBy() != null && po.getCreatedBy().getUser() != null
                ? po.getCreatedBy().getUser().getFullName()
                : "Unknown";

        return PurchaseOrderDTO.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .vendorId(po.getVendor() != null ? po.getVendor().getId() : null)
                .vendorName(po.getVendor() != null ? po.getVendor().getName() : "Unknown")
                .status(po.getStatus())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .notes(po.getNotes())
                .createdByName(createdByName)
                .createdAt(po.getCreatedAt())
                .items(itemDTOs)
                .totalValue(totalValue)
                .build();
    }

    private GoodsReceiptNoteDTO toGrnDTO(GoodsReceiptNote grn) {
        List<GrnLineItem> lineItems = grnLineItemRepository.findByGoodsReceiptNoteId(grn.getId());

        List<GrnLineItemDTO> lineDTOs = lineItems.stream().map(li -> {
            PurchaseOrderItem poItem = li.getPurchaseOrderItem();
            boolean hasDiscrepancy = li.getReceivedQuantity().compareTo(poItem.getOrderedQuantity()) != 0
                    || li.getCondition() != GrnItemCondition.GOOD;

            return GrnLineItemDTO.builder()
                    .id(li.getId())
                    .purchaseOrderItemId(poItem.getId())
                    .itemName(poItem.getItemNameSnapshot())
                    .orderedQuantity(poItem.getOrderedQuantity())
                    .receivedQuantity(li.getReceivedQuantity())
                    .unit(poItem.getUnit())
                    .condition(li.getCondition())
                    .discrepancyNote(li.getDiscrepancyNote())
                    .hasDiscrepancy(hasDiscrepancy)
                    .build();
        }).collect(Collectors.toList());

        boolean hasDiscrepancies = lineDTOs.stream().anyMatch(GrnLineItemDTO::isHasDiscrepancy);

        String receivedByName = grn.getReceivedBy() != null && grn.getReceivedBy().getUser() != null
                ? grn.getReceivedBy().getUser().getFullName()
                : "Unknown";

        String poNumber = grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getPoNumber() : "";
        String vendorName = grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getVendor() != null
                ? grn.getPurchaseOrder().getVendor().getName()
                : "";

        return GoodsReceiptNoteDTO.builder()
                .id(grn.getId())
                .purchaseOrderId(grn.getPurchaseOrder().getId())
                .poNumber(poNumber)
                .vendorName(vendorName)
                .invoiceReference(grn.getInvoiceReference())
                .receivedByName(receivedByName)
                .receivedAt(grn.getReceivedAt())
                .notes(grn.getNotes())
                .items(lineDTOs)
                .hasDiscrepancies(hasDiscrepancies)
                .build();
    }
}
