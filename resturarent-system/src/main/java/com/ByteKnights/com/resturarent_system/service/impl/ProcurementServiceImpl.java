package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.audit.Auditable;
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
import com.ByteKnights.com.resturarent_system.service.AuditLogService;
import com.ByteKnights.com.resturarent_system.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcurementServiceImpl implements ProcurementService {

    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final GrnLineItemRepository grnLineItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final StaffRepository staffRepository;
    private final ChefRequestRepository chefRequestRepository;
    private final com.ByteKnights.com.resturarent_system.repository.PurchaseOrderLogRepository purchaseOrderLogRepository;
    private final AuditLogService auditLogService;

    @jakarta.annotation.PostConstruct
    public void fixChefRequestStatusEnum() {
        try {
            jdbcTemplate.execute("ALTER TABLE chef_requests MODIFY COLUMN status VARCHAR(255) NOT NULL");
            System.out.println("Successfully altered chef_requests.status to VARCHAR(255)");
        } catch (Exception e) {
            System.out.println("Could not alter chef_requests.status: " + e.getMessage());
        }
    }

    private Staff resolveStaff(Long userId) {
        return staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user: " + userId));
    }

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
    @Auditable(
            module = AuditModule.PROCUREMENT,
            eventType = AuditEventType.VENDOR_CREATED,
            targetType = AuditTargetType.VENDOR,
            description = "Vendor created successfully",
            captureResultAsNewValue = false
    )
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

        /*
         * AOP audit is used because vendor creation is a simple action.
         * It avoids storing full JSON and saves audit storage.
         */
        return toVendorDTO(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    public VendorDTO updateVendor(Long vendorId, UpdateVendorRequest request, Long userId) {
        Staff staff = resolveStaff(userId);

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));

        ensureSameBranch(vendor.getBranch().getId(), staff.getBranch().getId(), "Vendor does not belong to your branch");

        Map<String, Object> oldValues = buildVendorAuditSnapshot(vendor);

        vendor.setName(request.getName());
        vendor.setContactPerson(request.getContactPerson());
        vendor.setPhone(request.getPhone());
        vendor.setEmail(request.getEmail());
        vendor.setAddress(request.getAddress());
        vendor.setCategory(request.getCategory());
        vendor.setActive(request.isActive());

        Vendor savedVendor = vendorRepository.save(vendor);

        auditLogService.logCurrentUserAction(
                AuditModule.PROCUREMENT,
                AuditEventType.VENDOR_UPDATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.VENDOR,
                savedVendor.getId(),
                getVendorBranchId(savedVendor),
                "Vendor updated successfully",
                oldValues,
                buildVendorAuditSnapshot(savedVendor)
        );

        return toVendorDTO(savedVendor);
    }

    @Override
    @Transactional
    public void deactivateVendor(Long vendorId, Long userId) {
        Staff staff = resolveStaff(userId);

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found with id: " + vendorId));

        ensureSameBranch(vendor.getBranch().getId(), staff.getBranch().getId(), "Vendor does not belong to your branch");

        Map<String, Object> oldValues = buildVendorAuditSnapshot(vendor);

        vendor.setActive(false);

        Vendor savedVendor = vendorRepository.save(vendor);

        auditLogService.logCurrentUserAction(
                AuditModule.PROCUREMENT,
                AuditEventType.VENDOR_DEACTIVATED,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.VENDOR,
                savedVendor.getId(),
                getVendorBranchId(savedVendor),
                "Vendor deactivated successfully",
                oldValues,
                buildVendorAuditSnapshot(savedVendor)
        );
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

        ensureSameBranch(vendor.getBranch().getId(), staff.getBranch().getId(), "Vendor does not belong to your branch");

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

        ChefRequest linkedChefRequest = null;

        if (request.getChefRequestId() != null) {
            linkedChefRequest = chefRequestRepository.findById(request.getChefRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Chef request not found with id: " + request.getChefRequestId()
                    ));

            if (!linkedChefRequest.getBranch().getId().equals(staff.getBranch().getId())) {
                throw new RuntimeException("Chef request does not belong to your branch");
            }

            linkedChefRequest.setStatus(ChefRequestStatus.ORDERED);
            linkedChefRequest = chefRequestRepository.save(linkedChefRequest);
        }

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

        PurchaseOrderLog log = PurchaseOrderLog.builder()
                .purchaseOrder(savedPo)
                .status(PurchaseOrderStatus.SUBMITTED)
                .actionBy(staff)
                .build();

        purchaseOrderLogRepository.save(log);

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("purchaseOrder", buildPurchaseOrderAuditSnapshot(savedPo));
        newValues.put("linkedChefRequest", buildChefRequestAuditSnapshot(linkedChefRequest));

        auditLogService.logCurrentUserAction(
                AuditModule.PROCUREMENT,
                AuditEventType.PURCHASE_ORDER_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.PURCHASE_ORDER,
                savedPo.getId(),
                getPurchaseOrderBranchId(savedPo),
                "Purchase order created successfully",
                null,
                newValues
        );

        return getPurchaseOrderById(savedPo.getId());
    }

    @Override
    @Transactional
    public void cancelPurchaseOrder(Long poId, Long userId) {
        Staff staff = resolveStaff(userId);

        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + poId));

        ensureSameBranch(po.getBranch().getId(), staff.getBranch().getId(), "Purchase order does not belong to your branch");

        if (po.getStatus() != PurchaseOrderStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED purchase orders can be cancelled.");
        }

        Map<String, Object> oldValues = buildPurchaseOrderAuditSnapshot(po);

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        PurchaseOrderLog log = PurchaseOrderLog.builder()
                .purchaseOrder(savedPo)
                .status(PurchaseOrderStatus.CANCELLED)
                .actionBy(staff)
                .build();

        purchaseOrderLogRepository.save(log);

        auditLogService.logCurrentUserAction(
                AuditModule.PROCUREMENT,
                AuditEventType.PURCHASE_ORDER_CANCELLED,
                AuditStatus.SUCCESS,
                AuditSeverity.WARN,
                AuditTargetType.PURCHASE_ORDER,
                savedPo.getId(),
                getPurchaseOrderBranchId(savedPo),
                "Purchase order cancelled successfully",
                oldValues,
                buildPurchaseOrderAuditSnapshot(savedPo)
        );
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public PurchaseOrderDTO getPurchaseOrderById(Long poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + poId));

        return toPurchaseOrderDTO(po);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GOODS RECEIPT NOTE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GoodsReceiptNoteDTO createGrn(CreateGrnRequest request, Long userId) {
        Staff staff = resolveStaff(userId);

        PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase order not found with id: " + request.getPurchaseOrderId()
                ));

        ensureSameBranch(po.getBranch().getId(), staff.getBranch().getId(), "Purchase order does not belong to your branch");

        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("purchaseOrder", buildPurchaseOrderAuditSnapshot(po));

        GoodsReceiptNote grn = GoodsReceiptNote.builder()
                .purchaseOrder(po)
                .invoiceReference(request.getInvoiceReference())
                .receivedBy(staff)
                .notes(request.getNotes())
                .build();

        GoodsReceiptNote savedGrn = goodsReceiptNoteRepository.save(grn);

        List<Map<String, Object>> inventoryChanges = new ArrayList<>();

        for (GrnLineItemRequest lineReq : request.getItems()) {
            PurchaseOrderItem poItem = purchaseOrderItemRepository.findById(lineReq.getPurchaseOrderItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "PO line item not found: " + lineReq.getPurchaseOrderItemId()
                    ));

            GrnItemCondition condition = lineReq.getCondition() != null
                    ? lineReq.getCondition()
                    : GrnItemCondition.GOOD;

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

            if (condition == GrnItemCondition.GOOD
                    && lineReq.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0
                    && poItem.getInventoryItem() != null) {

                InventoryItem invItem = poItem.getInventoryItem();

                Map<String, Object> inventoryOldValues = buildInventoryItemAuditSnapshot(invItem);

                BigDecimal previousQty = invItem.getQuantity() != null ? invItem.getQuantity() : BigDecimal.ZERO;
                BigDecimal newQty = previousQty.add(lineReq.getReceivedQuantity());

                invItem.setQuantity(newQty);
                InventoryItem savedInventoryItem = inventoryItemRepository.save(invItem);

                InventoryTransaction transaction = InventoryTransaction.builder()
                        .inventoryItem(savedInventoryItem)
                        .staff(staff)
                        .transactionType(InventoryTransactionType.RESTOCK)
                        .quantityChange(lineReq.getReceivedQuantity())
                        .previousQuantity(previousQty)
                        .newQuantity(newQty)
                        .unitPrice(poItem.getAgreedUnitPrice())
                        .notes("Auto-restocked via GRN for PO: " + po.getPoNumber())
                        .build();

                InventoryTransaction savedTransaction = inventoryTransactionRepository.save(transaction);

                Map<String, Object> inventoryChange = new LinkedHashMap<>();
                inventoryChange.put("oldInventoryItem", inventoryOldValues);
                inventoryChange.put("newInventoryItem", buildInventoryItemAuditSnapshot(savedInventoryItem));
                inventoryChange.put("inventoryTransaction", buildInventoryTransactionAuditSnapshot(savedTransaction));

                inventoryChanges.add(inventoryChange);
            }
        }

        updatePoStatusAfterGrn(po, staff);

        PurchaseOrder updatedPo = purchaseOrderRepository.findById(po.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + po.getId()));

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("goodsReceiptNote", buildGrnAuditSnapshot(savedGrn));
        newValues.put("purchaseOrder", buildPurchaseOrderAuditSnapshot(updatedPo));
        newValues.put("inventoryChanges", inventoryChanges);

        auditLogService.logCurrentUserAction(
                AuditModule.PROCUREMENT,
                AuditEventType.GOODS_RECEIPT_NOTE_CREATED,
                AuditStatus.SUCCESS,
                AuditSeverity.INFO,
                AuditTargetType.GOODS_RECEIPT_NOTE,
                savedGrn.getId(),
                getPurchaseOrderBranchId(updatedPo),
                "Goods receipt note created successfully",
                oldValues,
                newValues
        );

        return getGrnById(savedGrn.getId());
    }

    private void updatePoStatusAfterGrn(PurchaseOrder po, Staff staff) {
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

        PurchaseOrderStatus oldStatus = po.getStatus();

        if (allFullyReceived) {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
        } else if (anyReceived) {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        if (oldStatus != po.getStatus()) {
            purchaseOrderRepository.save(po);

            PurchaseOrderLog log = PurchaseOrderLog.builder()
                    .purchaseOrder(po)
                    .status(po.getStatus())
                    .actionBy(staff)
                    .build();

            purchaseOrderLogRepository.save(log);
        } else {
            purchaseOrderRepository.save(po);
        }
    }

    @Override
    public List<GoodsReceiptNoteDTO> getGrnHistory(Long branchId) {
        return goodsReceiptNoteRepository.findByPurchaseOrderBranchIdOrderByReceivedAtDesc(branchId)
                .stream()
                .map(this::toGrnDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<com.ByteKnights.com.resturarent_system.dto.response.procurement.PurchaseOrderLogDTO> getPurchaseOrderLogs(Long branchId) {
        return purchaseOrderLogRepository.findByBranchIdOrderByCreatedAtDesc(branchId)
                .stream()
                .map(log -> {
                    String itemNames = purchaseOrderItemRepository.findByPurchaseOrderId(log.getPurchaseOrder().getId())
                            .stream()
                            .map(PurchaseOrderItem::getItemNameSnapshot)
                            .collect(Collectors.joining(", "));
                            
                    return com.ByteKnights.com.resturarent_system.dto.response.procurement.PurchaseOrderLogDTO.builder()
                            .id(log.getId())
                            .purchaseOrderId(log.getPurchaseOrder().getId())
                            .poNumber(log.getPurchaseOrder().getPoNumber())
                            .vendorName(log.getPurchaseOrder().getVendor().getName())
                            .status(log.getStatus())
                            .actionByName(log.getActionBy().getDisplayName())
                            .items(itemNames.isEmpty() ? "N/A" : itemNames)
                            .createdAt(log.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public GoodsReceiptNoteDTO getGrnById(Long grnId) {
        GoodsReceiptNote grn = goodsReceiptNoteRepository.findById(grnId)
                .orElseThrow(() -> new ResourceNotFoundException("GRN not found with id: " + grnId));

        return toGrnDTO(grn);
    }

    @Override
    public ProcurementSummaryDTO getProcurementSummary(Long branchId) {
        long totalActiveVendors = vendorRepository.countByBranchIdAndIsActiveTrue(branchId);
        long activePendingPos = purchaseOrderRepository.countActivePendingByBranchId(branchId);

        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

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
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────────────

    private VendorDTO toVendorDTO(Vendor vendor) {
        long activePoCount = purchaseOrderRepository.countByVendorIdAndStatus(
                vendor.getId(), PurchaseOrderStatus.SUBMITTED)
                + purchaseOrderRepository.countByVendorIdAndStatus(
                vendor.getId(), PurchaseOrderStatus.PARTIALLY_RECEIVED);

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

    // ─────────────────────────────────────────────────────────────────────────
    // AUDIT HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void ensureSameBranch(Long entityBranchId, Long staffBranchId, String message) {
        if (entityBranchId == null || staffBranchId == null || !entityBranchId.equals(staffBranchId)) {
            throw new RuntimeException(message);
        }
    }

    private Map<String, Object> buildVendorAuditSnapshot(Vendor vendor) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (vendor == null) {
            return snapshot;
        }

        snapshot.put("vendorId", vendor.getId());
        snapshot.put("branchId", vendor.getBranch() != null ? vendor.getBranch().getId() : null);
        snapshot.put("branchName", vendor.getBranch() != null ? vendor.getBranch().getName() : null);
        snapshot.put("name", vendor.getName());
        snapshot.put("contactPerson", vendor.getContactPerson());
        snapshot.put("phone", vendor.getPhone());
        snapshot.put("email", vendor.getEmail());
        snapshot.put("address", vendor.getAddress());
        snapshot.put("category", vendor.getCategory());
        snapshot.put("active", vendor.isActive());

        return snapshot;
    }

    private Map<String, Object> buildPurchaseOrderAuditSnapshot(PurchaseOrder po) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (po == null) {
            return snapshot;
        }

        snapshot.put("purchaseOrderId", po.getId());
        snapshot.put("poNumber", po.getPoNumber());
        snapshot.put("branchId", po.getBranch() != null ? po.getBranch().getId() : null);
        snapshot.put("branchName", po.getBranch() != null ? po.getBranch().getName() : null);

        snapshot.put("vendorId", po.getVendor() != null ? po.getVendor().getId() : null);
        snapshot.put("vendorName", po.getVendor() != null ? po.getVendor().getName() : null);

        snapshot.put("status", po.getStatus() != null ? po.getStatus().name() : null);
        snapshot.put("expectedDeliveryDate", po.getExpectedDeliveryDate());
        snapshot.put("notes", po.getNotes());

        snapshot.put("createdByStaffId", po.getCreatedBy() != null ? po.getCreatedBy().getId() : null);
        snapshot.put("createdByName",
                po.getCreatedBy() != null && po.getCreatedBy().getUser() != null
                        ? po.getCreatedBy().getUser().getFullName()
                        : null);

        snapshot.put("createdAt", po.getCreatedAt());

        List<Map<String, Object>> itemSnapshots = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId())
                .stream()
                .map(this::buildPurchaseOrderItemAuditSnapshot)
                .collect(Collectors.toList());

        snapshot.put("items", itemSnapshots);

        return snapshot;
    }

    private Map<String, Object> buildPurchaseOrderItemAuditSnapshot(PurchaseOrderItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (item == null) {
            return snapshot;
        }

        snapshot.put("purchaseOrderItemId", item.getId());
        snapshot.put("inventoryItemId",
                item.getInventoryItem() != null ? item.getInventoryItem().getId() : null);
        snapshot.put("itemNameSnapshot", item.getItemNameSnapshot());
        snapshot.put("orderedQuantity", item.getOrderedQuantity());
        snapshot.put("unit", item.getUnit());
        snapshot.put("agreedUnitPrice", item.getAgreedUnitPrice());

        return snapshot;
    }

    private Map<String, Object> buildGrnAuditSnapshot(GoodsReceiptNote grn) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (grn == null) {
            return snapshot;
        }

        snapshot.put("grnId", grn.getId());
        snapshot.put("purchaseOrderId", grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getId() : null);
        snapshot.put("poNumber", grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getPoNumber() : null);
        snapshot.put("branchId",
                grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getBranch() != null
                        ? grn.getPurchaseOrder().getBranch().getId()
                        : null);
        snapshot.put("invoiceReference", grn.getInvoiceReference());
        snapshot.put("notes", grn.getNotes());
        snapshot.put("receivedAt", grn.getReceivedAt());

        snapshot.put("receivedByStaffId", grn.getReceivedBy() != null ? grn.getReceivedBy().getId() : null);
        snapshot.put("receivedByName",
                grn.getReceivedBy() != null && grn.getReceivedBy().getUser() != null
                        ? grn.getReceivedBy().getUser().getFullName()
                        : null);

        List<Map<String, Object>> lineItems = grnLineItemRepository.findByGoodsReceiptNoteId(grn.getId())
                .stream()
                .map(this::buildGrnLineItemAuditSnapshot)
                .collect(Collectors.toList());

        snapshot.put("lineItems", lineItems);

        return snapshot;
    }

    private Map<String, Object> buildGrnLineItemAuditSnapshot(GrnLineItem lineItem) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (lineItem == null) {
            return snapshot;
        }

        PurchaseOrderItem poItem = lineItem.getPurchaseOrderItem();

        snapshot.put("grnLineItemId", lineItem.getId());
        snapshot.put("purchaseOrderItemId", poItem != null ? poItem.getId() : null);
        snapshot.put("itemName", poItem != null ? poItem.getItemNameSnapshot() : null);
        snapshot.put("orderedQuantity", poItem != null ? poItem.getOrderedQuantity() : null);
        snapshot.put("receivedQuantity", lineItem.getReceivedQuantity());
        snapshot.put("unit", poItem != null ? poItem.getUnit() : null);
        snapshot.put("condition", lineItem.getCondition() != null ? lineItem.getCondition().name() : null);
        snapshot.put("discrepancyNote", lineItem.getDiscrepancyNote());

        return snapshot;
    }

    private Map<String, Object> buildInventoryItemAuditSnapshot(InventoryItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (item == null) {
            return snapshot;
        }

        snapshot.put("inventoryItemId", item.getId());
        snapshot.put("name", item.getName());
        snapshot.put("category", item.getCategory());
        snapshot.put("quantity", item.getQuantity());
        snapshot.put("unit", item.getUnit());
        snapshot.put("unitPrice", item.getUnitPrice());
        snapshot.put("branchId", item.getBranch() != null ? item.getBranch().getId() : null);

        return snapshot;
    }

    private Map<String, Object> buildInventoryTransactionAuditSnapshot(InventoryTransaction transaction) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (transaction == null) {
            return snapshot;
        }

        snapshot.put("transactionId", transaction.getId());
        snapshot.put("inventoryItemId",
                transaction.getInventoryItem() != null ? transaction.getInventoryItem().getId() : null);
        snapshot.put("transactionType",
                transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null);
        snapshot.put("quantityChange", transaction.getQuantityChange());
        snapshot.put("previousQuantity", transaction.getPreviousQuantity());
        snapshot.put("newQuantity", transaction.getNewQuantity());
        snapshot.put("unitPrice", transaction.getUnitPrice());
        snapshot.put("notes", transaction.getNotes());
        snapshot.put("createdAt", transaction.getCreatedAt());

        return snapshot;
    }

    private Map<String, Object> buildChefRequestAuditSnapshot(ChefRequest request) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        if (request == null) {
            return snapshot;
        }

        snapshot.put("chefRequestId", request.getId());
        snapshot.put("branchId", request.getBranch() != null ? request.getBranch().getId() : null);
        snapshot.put("chefName", request.getChefName());
        snapshot.put("itemName", request.getItemName());
        snapshot.put("requestedQuantity", request.getRequestedQuantity());
        snapshot.put("unit", request.getUnit());
        snapshot.put("status", request.getStatus() != null ? request.getStatus().name() : null);
        snapshot.put("requestType", request.getRequestType() != null ? request.getRequestType().name() : null);

        return snapshot;
    }

    private Long getVendorBranchId(Vendor vendor) {
        if (vendor == null || vendor.getBranch() == null) {
            return null;
        }

        return vendor.getBranch().getId();
    }

    private Long getPurchaseOrderBranchId(PurchaseOrder po) {
        if (po == null || po.getBranch() == null) {
            return null;
        }

        return po.getBranch().getId();
    }
}