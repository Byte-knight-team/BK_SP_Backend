package com.ByteKnights.com.resturarent_system.controller;

import com.ByteKnights.com.resturarent_system.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * REST Controller that exposes endpoints for downloading branch-specific
 * PDF reports. Each endpoint delegates to the ReportService for data
 * aggregation and PDF generation, then returns the PDF bytes as a
 * downloadable file attachment.
 *
 * All endpoints require the VIEW_ANALYTICS authority.
 * Base path: /api/manager/reports
 */
@RestController
@RequestMapping("/api/manager/reports")
@CrossOrigin
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─────────────────────────────────────────────────────────────
    // Helper: build a PDF response with a descriptive filename
    // ─────────────────────────────────────────────────────────────
    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdf.length);
        headers.setAccessControlExposeHeaders(java.util.List.of("Content-Disposition"));
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Sales Report
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getSalesReport(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        byte[] pdf = reportService.generateSalesReport(branchId, userId, startDate, endDate);
        String filename = "sales-report-" + startDate.format(FILE_DATE_FMT) + "-to-" + endDate.format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Revenue Trend Report
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/revenue-trend")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getRevenueTrendReport(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        byte[] pdf = reportService.generateRevenueTrendReport(branchId, userId, startDate, endDate);
        String filename = "revenue-trend-" + startDate.format(FILE_DATE_FMT) + "-to-" + endDate.format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Top Selling Items Report
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/top-selling-items")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getTopSellingItemsReport(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        byte[] pdf = reportService.generateTopSellingItemsReport(branchId, userId, startDate, endDate);
        String filename = "top-selling-items-" + startDate.format(FILE_DATE_FMT) + "-to-" + endDate.format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Order Summary Report
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/order-summary")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getOrderSummaryReport(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        byte[] pdf = reportService.generateOrderSummaryReport(branchId, userId, startDate, endDate);
        String filename = "order-summary-" + startDate.format(FILE_DATE_FMT) + "-to-" + endDate.format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Delivery Performance Report
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/delivery-performance")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getDeliveryPerformanceReport(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        byte[] pdf = reportService.generateDeliveryPerformanceReport(branchId, userId, startDate, endDate);
        String filename = "delivery-performance-" + startDate.format(FILE_DATE_FMT) + "-to-" + endDate.format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 6. Reservation Report
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/reservations")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getReservationReport(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        byte[] pdf = reportService.generateReservationReport(branchId, userId, startDate, endDate);
        String filename = "reservation-report-" + startDate.format(FILE_DATE_FMT) + "-to-" + endDate.format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 7. Inventory Status Report
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/inventory-status")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getInventoryStatusReport(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        byte[] pdf = reportService.generateInventoryStatusReport(branchId, userId, startDate, endDate);
        String filename = "inventory-status-" + startDate.format(FILE_DATE_FMT) + "-to-" + endDate.format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 8. Procurement & PO Report
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/procurement")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getProcurementReport(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        byte[] pdf = reportService.generateProcurementReport(branchId, userId, startDate, endDate);
        String filename = "procurement-report-" + startDate.format(FILE_DATE_FMT) + "-to-" + endDate.format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 9. Staff Details Report (snapshot — no date filter)
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/staff-details")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getStaffDetailsReport(
            @RequestParam Long branchId,
            @RequestParam Long userId) {

        byte[] pdf = reportService.generateStaffDetailsReport(branchId, userId);
        String filename = "staff-details-" + LocalDate.now().format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }

    // ─────────────────────────────────────────────────────────────
    // 10. Customer Reviews Report
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/customer-reviews")
    @PreAuthorize("hasAuthority('VIEW_ANALYTICS')")
    public ResponseEntity<byte[]> getCustomerReviewsReport(
            @RequestParam Long branchId,
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        byte[] pdf = reportService.generateCustomerReviewsReport(branchId, userId, startDate, endDate);
        String filename = "customer-reviews-" + startDate.format(FILE_DATE_FMT) + "-to-" + endDate.format(FILE_DATE_FMT) + ".pdf";
        return pdfResponse(pdf, filename);
    }
}
