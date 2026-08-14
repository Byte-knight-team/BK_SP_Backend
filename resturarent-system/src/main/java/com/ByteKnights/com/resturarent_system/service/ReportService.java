package com.ByteKnights.com.resturarent_system.service;

import java.time.LocalDate;

/**
 * Service interface for generating downloadable PDF reports.
 *
 * Each method queries the relevant data from the database, generates a
 * professionally formatted PDF document, and returns it as a byte array
 * ready to be served as an HTTP response.
 */
public interface ReportService {

    /**
     * Generates a Sales Report (Gross Sales, Refunds, Net Sales, Payment Breakdown, Channel Breakdown).
     */
    byte[] generateSalesReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Generates a Revenue Trend Report (daily revenue and order counts over the date range).
     */
    byte[] generateRevenueTrendReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Generates a Top Selling Items Report (ranked list of items by quantity sold and revenue).
     */
    byte[] generateTopSellingItemsReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Generates an Order Summary Report (status breakdown, cancellation reasons, peak hours).
     */
    byte[] generateOrderSummaryReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Generates a Delivery Performance Report (driver stats, avg delivery time, cancellations).
     */
    byte[] generateDeliveryPerformanceReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Generates a Reservation Report (status breakdown, deposit revenue, daily volumes).
     */
    byte[] generateReservationReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Generates an Inventory Status Report (current stock levels, low stock items, transactions).
     * Supports date filtering on the inventory transaction log.
     */
    byte[] generateInventoryStatusReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Generates a Procurement & PO Report (PO status, vendor spend, GRN summary).
     */
    byte[] generateProcurementReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Generates a Staff Details Report (directory snapshot of all current staff members).
     */
    byte[] generateStaffDetailsReport(Long branchId, Long userId);

    /**
     * Generates a Customer Reviews and Feedback Report (rating distribution, recent reviews).
     */
    byte[] generateCustomerReviewsReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate);
}
