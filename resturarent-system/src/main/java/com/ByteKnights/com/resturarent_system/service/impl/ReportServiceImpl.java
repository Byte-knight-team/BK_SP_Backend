package com.ByteKnights.com.resturarent_system.service.impl;

import com.ByteKnights.com.resturarent_system.entity.*;
import com.ByteKnights.com.resturarent_system.repository.*;
import com.ByteKnights.com.resturarent_system.service.ReportService;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of ReportService.
 *
 * Uses the OpenPDF library (fork of iText 4, LGPL licensed) to generate
 * professionally formatted PDF reports. All reports share a consistent
 * visual style through shared helper methods.
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    // ─────────────────────────────────────────────────────────────────────────
    // INJECTED REPOSITORIES
    // ─────────────────────────────────────────────────────────────────────────
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final DeliveryRepository deliveryRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final GrnLineItemRepository grnLineItemRepository;
    private final StaffRepository staffRepository;
    private final ReviewRepository reviewRepository;
    private final BranchRepository branchRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // BRAND COLORS & FONTS
    // ─────────────────────────────────────────────────────────────────────────
    private static final Color BRAND_COLOR       = new Color(234, 88, 12);   // #ea580c (orange)
    private static final Color HEADER_BG         = new Color(249, 115, 22);  // slightly lighter orange
    private static final Color SUBHEADER_BG      = new Color(243, 244, 246); // gray-100
    private static final Color ALT_ROW_BG        = new Color(249, 250, 251); // gray-50
    private static final Color BORDER_COLOR      = new Color(229, 231, 235); // gray-200
    private static final Color TEXT_DARK         = new Color(17, 24, 39);    // gray-900
    private static final Color TEXT_MUTED        = new Color(107, 114, 128); // gray-500
    private static final Color SUCCESS_COLOR     = new Color(22, 163, 74);   // green-600
    private static final Color DANGER_COLOR      = new Color(220, 38, 38);   // red-600
    private static final Color WARNING_COLOR     = new Color(217, 119, 6);   // amber-600

    private static final Font FONT_TITLE      = new Font(Font.HELVETICA, 20, Font.BOLD,   Color.WHITE);
    private static final Font FONT_SUBTITLE   = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.WHITE);
    private static final Font FONT_SECTION    = new Font(Font.HELVETICA, 12, Font.BOLD,   TEXT_DARK);
    private static final Font FONT_TABLE_HDR  = new Font(Font.HELVETICA,  9, Font.BOLD,   Color.WHITE);
    private static final Font FONT_TABLE_CELL = new Font(Font.HELVETICA,  9, Font.NORMAL, TEXT_DARK);
    private static final Font FONT_TABLE_BOLD = new Font(Font.HELVETICA,  9, Font.BOLD,   TEXT_DARK);
    private static final Font FONT_LABEL      = new Font(Font.HELVETICA,  9, Font.NORMAL, TEXT_MUTED);
    private static final Font FONT_VALUE      = new Font(Font.HELVETICA, 14, Font.BOLD,   TEXT_DARK);
    private static final Font FONT_SMALL      = new Font(Font.HELVETICA,  8, Font.NORMAL, TEXT_MUTED);

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT    = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─────────────────────────────────────────────────────────────────────────
    // PDF HELPER METHODS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new Document configured for A4 portrait with standard margins.
     */
    private Document createDocument() {
        return new Document(PageSize.A4, 36, 36, 36, 50);
    }

    /**
     * Adds a full-width branded header block to the document.
     * Contains the report title, branch name, date range, and generation timestamp.
     */
    private void addReportHeader(Document doc, String reportTitle, String branchName,
                                  LocalDate startDate, LocalDate endDate) throws DocumentException {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        header.setSpacingAfter(16);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BRAND_COLOR);
        cell.setPadding(16);
        cell.setBorder(Rectangle.NO_BORDER);

        Paragraph title = new Paragraph(reportTitle, FONT_TITLE);
        title.setSpacingAfter(4);
        cell.addElement(title);

        String subText = "Branch: " + branchName;
        if (startDate != null && endDate != null) {
            subText += "   |   Period: " + startDate.format(DATE_FMT) + " – " + endDate.format(DATE_FMT);
        }
        subText += "   |   Generated: " + LocalDateTime.now().format(DT_FMT);
        cell.addElement(new Paragraph(subText, FONT_SUBTITLE));

        header.addCell(cell);
        doc.add(header);
    }

    /**
     * Adds a full-width branded header block (snapshot — no date range).
     */
    private void addReportHeaderSnapshot(Document doc, String reportTitle, String branchName) throws DocumentException {
        addReportHeader(doc, reportTitle, branchName, null, null);
    }

    /**
     * Adds a section heading (bold orange label with a bottom border line).
     */
    private void addSectionHeading(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, FONT_SECTION);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        doc.add(p);

        // thin orange rule
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingAfter(8);
        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setFixedHeight(2f);
        ruleCell.setBackgroundColor(BRAND_COLOR);
        ruleCell.setBorder(Rectangle.NO_BORDER);
        rule.addCell(ruleCell);
        doc.add(rule);
    }

    /**
     * Adds a horizontal summary box with labelled metric cards.
     * @param labels  array of label strings
     * @param values  array of formatted value strings (same length as labels)
     */
    private void addSummaryBox(Document doc, String[] labels, String[] values) throws DocumentException {
        int cols = labels.length;
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);

        for (int i = 0; i < cols; i++) {
            PdfPCell cell = new PdfPCell();
            cell.setBorderColor(BORDER_COLOR);
            cell.setPadding(10);
            cell.setBackgroundColor(i % 2 == 0 ? Color.WHITE : ALT_ROW_BG);

            Paragraph label = new Paragraph(labels[i], FONT_LABEL);
            label.setSpacingAfter(4);
            cell.addElement(label);
            cell.addElement(new Paragraph(values[i], FONT_VALUE));

            table.addCell(cell);
        }
        doc.add(table);
    }

    /**
     * Creates a styled table header row (brand-orange background, white bold text).
     */
    private void addTableHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_TABLE_HDR));
            cell.setBackgroundColor(BRAND_COLOR);
            cell.setPadding(7);
            cell.setBorderColor(HEADER_BG);
            table.addCell(cell);
        }
    }

    /**
     * Adds a standard data row with optional alternating background.
     */
    private void addTableRow(PdfPTable table, boolean isAlt, String... values) {
        Color bg = isAlt ? ALT_ROW_BG : Color.WHITE;
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(v != null ? v : "—", FONT_TABLE_CELL));
            cell.setBackgroundColor(bg);
            cell.setPadding(6);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }
    }

    /**
     * Adds a data row with a custom font for the first cell (e.g., rank in bold).
     */
    private void addTableRowWithBoldFirst(PdfPTable table, boolean isAlt, String first, String... rest) {
        Color bg = isAlt ? ALT_ROW_BG : Color.WHITE;

        PdfPCell firstCell = new PdfPCell(new Phrase(first != null ? first : "—", FONT_TABLE_BOLD));
        firstCell.setBackgroundColor(bg);
        firstCell.setPadding(6);
        firstCell.setBorderColor(BORDER_COLOR);
        table.addCell(firstCell);

        for (String v : rest) {
            PdfPCell cell = new PdfPCell(new Phrase(v != null ? v : "—", FONT_TABLE_CELL));
            cell.setBackgroundColor(bg);
            cell.setPadding(6);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }
    }

    /**
     * Adds a "No data available" placeholder row spanning all columns.
     */
    private void addEmptyRow(PdfPTable table, int colSpan) {
        PdfPCell cell = new PdfPCell(new Phrase("No data available for the selected period.", FONT_SMALL));
        cell.setColspan(colSpan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(10);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(ALT_ROW_BG);
        table.addCell(cell);
    }

    /**
     * Adds a page footer event to number pages.
     * (Attached to PdfWriter before opening the document.)
     */
    private static class PageFooter extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase footer = new Phrase(
                "Page " + writer.getPageNumber() + "   |   Byte Knights Restaurant Management System",
                new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED)
            );
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                (document.right() - document.left()) / 2 + document.leftMargin(),
                document.bottom() - 10, 0);
        }
    }

    /**
     * Resolves the branch name for report headers.
     */
    private String resolveBranchName(Long branchId) {
        return branchRepository.findById(branchId)
                .map(b -> b.getName() != null ? b.getName() : "Branch #" + branchId)
                .orElse("Branch #" + branchId);
    }

    /**
     * Formats a BigDecimal as a currency string.
     */
    private String fmt(BigDecimal val) {
        if (val == null) return "Rs. 0.00";
        return "Rs. " + String.format("%,.2f", val.doubleValue());
    }

    /**
     * Formats a long as a plain number string.
     */
    private String fmtNum(long val) {
        return String.format("%,d", val);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REPORT IMPLEMENTATIONS — Phase 2, 3, 4 will fill these in
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generateSalesReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        String branchName = resolveBranchName(branchId);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findByBranchIdAndCreatedAtBetween(branchId, start, end);

        BigDecimal grossSales = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        BigDecimal taxCollected = BigDecimal.ZERO;
        BigDecimal deliveryFees = BigDecimal.ZERO;
        BigDecimal serviceCharges = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;

        Map<PaymentMethod, BigDecimal> paymentMethodRevenue = new EnumMap<>(PaymentMethod.class);
        Map<OrderType, BigDecimal> channelRevenue = new EnumMap<>(OrderType.class);
        Map<OrderType, Long> channelCount = new EnumMap<>(OrderType.class);

        for (Order o : orders) {
            // Group by Channel
            channelCount.put(o.getOrderType(), channelCount.getOrDefault(o.getOrderType(), 0L) + 1);

            if (o.getStatus() == OrderStatus.REFUNDED) {
                refunds = refunds.add(o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO);
            } else if (o.getPaymentStatus() == PaymentStatus.PAID || o.getPaymentStatus() == PaymentStatus.SUCCESS) {
                BigDecimal amt = o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO;
                grossSales = grossSales.add(amt);
                taxCollected = taxCollected.add(o.getTaxAmount() != null ? o.getTaxAmount() : BigDecimal.ZERO);
                deliveryFees = deliveryFees.add(o.getDeliveryFee() != null ? o.getDeliveryFee() : BigDecimal.ZERO);
                serviceCharges = serviceCharges.add(o.getServiceCharge() != null ? o.getServiceCharge() : BigDecimal.ZERO);
                discounts = discounts.add(o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO);

                channelRevenue.put(o.getOrderType(), channelRevenue.getOrDefault(o.getOrderType(), BigDecimal.ZERO).add(amt));

                // Payment Method
                Optional<Payment> paymentOpt = paymentRepository.findByOrder(o);
                if (paymentOpt.isPresent()) {
                    PaymentMethod method = paymentOpt.get().getPaymentMethod();
                    paymentMethodRevenue.put(method, paymentMethodRevenue.getOrDefault(method, BigDecimal.ZERO).add(amt));
                }
            }
        }

        BigDecimal netSales = grossSales.subtract(refunds);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = createDocument();
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PageFooter());
            doc.open();

            addReportHeader(doc, "Sales & Revenue Report", branchName, startDate, endDate);

            addSectionHeading(doc, "Financial Summary");
            addSummaryBox(doc,
                    new String[] { "Gross Sales", "Refunds", "Net Sales" },
                    new String[] { fmt(grossSales), fmt(refunds), fmt(netSales) });
            addSummaryBox(doc,
                    new String[] { "Tax Collected", "Delivery Fees", "Discounts Given" },
                    new String[] { fmt(taxCollected), fmt(deliveryFees), fmt(discounts) });

            addSectionHeading(doc, "Payment Method Breakdown");
            PdfPTable pt = new PdfPTable(3);
            pt.setWidthPercentage(100);
            addTableHeader(pt, "Payment Method", "Revenue", "% of Gross Sales");
            boolean alt = false;
            if (paymentMethodRevenue.isEmpty()) {
                addEmptyRow(pt, 3);
            } else {
                for (Map.Entry<PaymentMethod, BigDecimal> e : paymentMethodRevenue.entrySet()) {
                    BigDecimal rev = e.getValue();
                    String pct = grossSales.compareTo(BigDecimal.ZERO) == 0 ? "0%" :
                            String.format("%.1f%%", rev.multiply(new BigDecimal(100)).divide(grossSales, 1, java.math.RoundingMode.HALF_UP));
                    addTableRow(pt, alt, e.getKey().name(), fmt(rev), pct);
                    alt = !alt;
                }
            }
            doc.add(pt);

            addSectionHeading(doc, "Order Channel Breakdown");
            PdfPTable ct = new PdfPTable(4);
            ct.setWidthPercentage(100);
            addTableHeader(ct, "Channel (Type)", "Order Count", "Revenue", "% of Gross Sales");
            alt = false;
            if (channelCount.isEmpty()) {
                addEmptyRow(ct, 4);
            } else {
                for (Map.Entry<OrderType, Long> e : channelCount.entrySet()) {
                    OrderType type = e.getKey();
                    BigDecimal rev = channelRevenue.getOrDefault(type, BigDecimal.ZERO);
                    String pct = grossSales.compareTo(BigDecimal.ZERO) == 0 ? "0%" :
                            String.format("%.1f%%", rev.multiply(new BigDecimal(100)).divide(grossSales, 1, java.math.RoundingMode.HALF_UP));
                    addTableRow(ct, alt, type.name(), fmtNum(e.getValue()), fmt(rev), pct);
                    alt = !alt;
                }
            }
            doc.add(ct);

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new RuntimeException("Error generating Sales Report", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateRevenueTrendReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        String branchName = resolveBranchName(branchId);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Object[]> trendData = orderRepository.findRevenueTrendByBranchAndDates(branchId, start, end);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalOrders = 0;

        for (Object[] row : trendData) {
            BigDecimal rev = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            long count = row[2] != null ? Long.parseLong(row[2].toString()) : 0L;
            totalRevenue = totalRevenue.add(rev);
            totalOrders += count;
        }

        BigDecimal avgOrderValue = totalOrders == 0 ? BigDecimal.ZERO :
                totalRevenue.divide(new BigDecimal(totalOrders), 2, java.math.RoundingMode.HALF_UP);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = createDocument();
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PageFooter());
            doc.open();

            addReportHeader(doc, "Revenue Trend Report", branchName, startDate, endDate);

            addSectionHeading(doc, "Period Summary");
            addSummaryBox(doc,
                    new String[] { "Total Revenue", "Total Orders", "Avg Order Value" },
                    new String[] { fmt(totalRevenue), fmtNum(totalOrders), fmt(avgOrderValue) });

            addSectionHeading(doc, "Daily Breakdown");
            PdfPTable pt = new PdfPTable(4);
            pt.setWidthPercentage(100);
            addTableHeader(pt, "Date", "Orders", "Revenue", "Avg Value");

            boolean alt = false;
            if (trendData.isEmpty()) {
                addEmptyRow(pt, 4);
            } else {
                for (Object[] row : trendData) {
                    String date = row[0] != null ? row[0].toString() : "Unknown";
                    BigDecimal rev = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                    long count = row[2] != null ? Long.parseLong(row[2].toString()) : 0L;
                    BigDecimal avg = count == 0 ? BigDecimal.ZERO : rev.divide(new BigDecimal(count), 2, java.math.RoundingMode.HALF_UP);

                    addTableRow(pt, alt, date, fmtNum(count), fmt(rev), fmt(avg));
                    alt = !alt;
                }
            }
            doc.add(pt);

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new RuntimeException("Error generating Revenue Trend Report", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateTopSellingItemsReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implemented in Phase 2
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateOrderSummaryReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implemented in Phase 2
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateDeliveryPerformanceReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implemented in Phase 3
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReservationReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implemented in Phase 3
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInventoryStatusReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implemented in Phase 3
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateProcurementReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implemented in Phase 3
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStaffDetailsReport(Long branchId, Long userId) {
        // TODO: Implemented in Phase 4
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateCustomerReviewsReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        // TODO: Implemented in Phase 4
        return new byte[0];
    }
}
