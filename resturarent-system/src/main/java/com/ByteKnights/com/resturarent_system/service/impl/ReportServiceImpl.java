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

    /**
     * Generates a Sales Report in PDF format.
     * Calculates gross sales, refunds, net sales, taxes, fees, and discounts for a given period.
     * Also breaks down revenue by payment method (Card/Cash) and order channel.
     * 
     * @param branchId ID of the branch
     * @param userId ID of the requesting user
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return PDF byte array
     */
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

    /**
     * Generates a Revenue Trend Report in PDF format.
     * Displays daily order volumes, revenue, and average order value across the selected date range.
     * 
     * @param branchId ID of the branch
     * @param userId ID of the requesting user
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return PDF byte array
     */
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

    /**
     * Generates a Top Selling Items Report in PDF format.
     * Aggregates all order items sold within the time frame, computes totals,
     * and ranks them descending by revenue generated.
     * 
     * @param branchId ID of the branch
     * @param userId ID of the requesting user
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return PDF byte array
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] generateTopSellingItemsReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        String branchName = resolveBranchName(branchId);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findByBranchIdAndPaymentStatusInAndCreatedAtBetween(
                branchId, Arrays.asList(PaymentStatus.PAID, PaymentStatus.SUCCESS), start, end);
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalItemsSold = 0;

        Map<String, BigDecimal> itemRevenue = new HashMap<>();
        Map<String, Long> itemQuantity = new HashMap<>();

        if (!orderIds.isEmpty()) {
            List<OrderItem> orderItems = orderItemRepository.findByOrderIdIn(orderIds);
            for (OrderItem oi : orderItems) {
                String name = oi.getItemName();
                BigDecimal subtotal = oi.getSubtotal() != null ? oi.getSubtotal() : BigDecimal.ZERO;
                long qty = oi.getQuantity() != null ? oi.getQuantity() : 0L;

                itemRevenue.put(name, itemRevenue.getOrDefault(name, BigDecimal.ZERO).add(subtotal));
                itemQuantity.put(name, itemQuantity.getOrDefault(name, 0L) + qty);

                totalRevenue = totalRevenue.add(subtotal);
                totalItemsSold += qty;
            }
        }

        List<Map.Entry<String, BigDecimal>> sortedItems = new ArrayList<>(itemRevenue.entrySet());
        sortedItems.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = createDocument();
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PageFooter());
            doc.open();

            addReportHeader(doc, "Top Selling Items Report", branchName, startDate, endDate);

            addSectionHeading(doc, "Period Summary");
            addSummaryBox(doc,
                    new String[] { "Total Items Sold", "Total Item Revenue" },
                    new String[] { fmtNum(totalItemsSold), fmt(totalRevenue) });

            addSectionHeading(doc, "Ranked Items");
            PdfPTable pt = new PdfPTable(new float[]{1, 4, 2, 2, 2});
            pt.setWidthPercentage(100);
            addTableHeader(pt, "Rank", "Item Name", "Qty Sold", "Revenue", "% of Revenue");

            boolean alt = false;
            if (sortedItems.isEmpty()) {
                addEmptyRow(pt, 5);
            } else {
                int rank = 1;
                for (Map.Entry<String, BigDecimal> e : sortedItems) {
                    String name = e.getKey();
                    BigDecimal rev = e.getValue();
                    long qty = itemQuantity.getOrDefault(name, 0L);
                    String pct = totalRevenue.compareTo(BigDecimal.ZERO) == 0 ? "0%" :
                            String.format("%.1f%%", rev.multiply(new BigDecimal(100)).divide(totalRevenue, 1, java.math.RoundingMode.HALF_UP));

                    addTableRowWithBoldFirst(pt, alt, "#" + rank, name, fmtNum(qty), fmt(rev), pct);
                    rank++;
                    alt = !alt;
                }
            }
            doc.add(pt);

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new RuntimeException("Error generating Top Selling Items Report", e);
        }
    }

    /**
     * Generates an Order Summary Report in PDF format.
     * Provides high-level operational insights including completion/cancellation rates,
     * common cancellation reasons, average preparation times, and peak ordering hours.
     * 
     * @param branchId ID of the branch
     * @param userId ID of the requesting user
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return PDF byte array
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] generateOrderSummaryReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        String branchName = resolveBranchName(branchId);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findByBranchIdAndCreatedAtBetween(branchId, start, end);

        long totalOrders = orders.size();
        long completed = 0;
        long cancelled = 0;

        long totalPrepSeconds = 0;
        long prepCount = 0;

        Map<OrderStatus, Long> statusCount = new EnumMap<>(OrderStatus.class);
        Map<String, Long> cancelReasons = new HashMap<>();
        Map<OrderType, Long> typeCount = new EnumMap<>(OrderType.class);
        Map<OrderType, BigDecimal> typeRevenue = new EnumMap<>(OrderType.class);
        Map<String, Long> hourCount = new TreeMap<>();

        for (Order o : orders) {
            statusCount.put(o.getStatus(), statusCount.getOrDefault(o.getStatus(), 0L) + 1);
            typeCount.put(o.getOrderType(), typeCount.getOrDefault(o.getOrderType(), 0L) + 1);

            if (o.getPaymentStatus() == PaymentStatus.PAID || o.getPaymentStatus() == PaymentStatus.SUCCESS) {
                BigDecimal amt = o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO;
                typeRevenue.put(o.getOrderType(), typeRevenue.getOrDefault(o.getOrderType(), BigDecimal.ZERO).add(amt));
            }

            if (o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.SERVED) completed++;
            if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.REJECTED) {
                cancelled++;
                String reason = o.getCancelReason() != null && !o.getCancelReason().trim().isEmpty() ? 
                        o.getCancelReason() : "No reason provided";
                cancelReasons.put(reason, cancelReasons.getOrDefault(reason, 0L) + 1);
            }

            if (o.getCookingStartedAt() != null && o.getCookingCompletedAt() != null) {
                long secs = java.time.Duration.between(o.getCookingStartedAt(), o.getCookingCompletedAt()).getSeconds();
                if (secs > 0) {
                    totalPrepSeconds += secs;
                    prepCount++;
                }
            }

            String hourLabel = String.format("%02d:00", o.getCreatedAt().getHour());
            hourCount.put(hourLabel, hourCount.getOrDefault(hourLabel, 0L) + 1);
        }

        String avgPrepTime = prepCount == 0 ? "0 mins" : (totalPrepSeconds / prepCount / 60) + " mins";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = createDocument();
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PageFooter());
            doc.open();

            addReportHeader(doc, "Order Summary Report", branchName, startDate, endDate);

            addSectionHeading(doc, "Overview");
            addSummaryBox(doc,
                    new String[] { "Total Orders", "Completed", "Cancelled", "Avg Prep Time" },
                    new String[] { fmtNum(totalOrders), fmtNum(completed), fmtNum(cancelled), avgPrepTime });

            addSectionHeading(doc, "Order Status Breakdown");
            PdfPTable ptStatus = new PdfPTable(3);
            ptStatus.setWidthPercentage(100);
            addTableHeader(ptStatus, "Status", "Order Count", "% of Total");
            boolean alt = false;
            for (Map.Entry<OrderStatus, Long> e : statusCount.entrySet()) {
                String pct = totalOrders == 0 ? "0%" : String.format("%.1f%%", (e.getValue() * 100.0) / totalOrders);
                addTableRow(ptStatus, alt, e.getKey().name(), fmtNum(e.getValue()), pct);
                alt = !alt;
            }
            if (statusCount.isEmpty()) addEmptyRow(ptStatus, 3);
            doc.add(ptStatus);

            addSectionHeading(doc, "Cancellation Reasons");
            PdfPTable ptCancel = new PdfPTable(2);
            ptCancel.setWidthPercentage(100);
            addTableHeader(ptCancel, "Reason", "Count");
            alt = false;
            if (cancelReasons.isEmpty()) {
                PdfPCell cell = new PdfPCell(new Phrase("No cancellations found.", FONT_SMALL));
                cell.setColspan(2);
                cell.setPadding(10);
                cell.setBackgroundColor(ALT_ROW_BG);
                ptCancel.addCell(cell);
            } else {
                for (Map.Entry<String, Long> e : cancelReasons.entrySet()) {
                    addTableRow(ptCancel, alt, e.getKey(), fmtNum(e.getValue()));
                    alt = !alt;
                }
            }
            doc.add(ptCancel);

            addSectionHeading(doc, "Peak Ordering Hours");
            PdfPTable ptHour = new PdfPTable(2);
            ptHour.setWidthPercentage(100);
            addTableHeader(ptHour, "Hour", "Order Count");
            alt = false;
            if (hourCount.isEmpty()) {
                addEmptyRow(ptHour, 2);
            } else {
                for (Map.Entry<String, Long> e : hourCount.entrySet()) {
                    addTableRow(ptHour, alt, e.getKey(), fmtNum(e.getValue()));
                    alt = !alt;
                }
            }
            doc.add(ptHour);

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new RuntimeException("Error generating Order Summary Report", e);
        }
    }

    /**
     * Generates a Delivery Performance Report in PDF format.
     * Filters for delivery orders and evaluates overall delivery volumes,
     * cancellations, and aggregates performance metrics per driver.
     * 
     * @param branchId ID of the branch
     * @param userId ID of the requesting user
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return PDF byte array
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] generateDeliveryPerformanceReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        String branchName = resolveBranchName(branchId);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Order> orders = orderRepository.findByBranchIdAndCreatedAtBetween(branchId, start, end).stream()
                .filter(o -> o.getOrderType() == OrderType.ONLINE_DELIVERY)
                .collect(Collectors.toList());

        long totalDeliveries = orders.size();
        long delivered = 0;
        long cancelled = 0;

        Map<String, Long> driverDeliveries = new HashMap<>();
        Map<String, Long> driverCancellations = new HashMap<>();

        for (Order o : orders) {
            String driverName = "Unassigned";
            if (o.getAssignedDelivery() != null) {
                driverName = o.getAssignedDelivery().getFirstName() + " " + o.getAssignedDelivery().getLastName();
            }

            if (o.getStatus() == OrderStatus.ARRIVED || o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.SERVED) {
                delivered++;
                driverDeliveries.put(driverName, driverDeliveries.getOrDefault(driverName, 0L) + 1);
            } else if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.REJECTED) {
                cancelled++;
                driverCancellations.put(driverName, driverCancellations.getOrDefault(driverName, 0L) + 1);
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = createDocument();
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PageFooter());
            doc.open();

            addReportHeader(doc, "Delivery Performance Report", branchName, startDate, endDate);

            addSectionHeading(doc, "Overall Performance");
            addSummaryBox(doc,
                    new String[] { "Total Delivery Orders", "Successful Deliveries", "Cancelled Deliveries" },
                    new String[] { fmtNum(totalDeliveries), fmtNum(delivered), fmtNum(cancelled) });

            addSectionHeading(doc, "Driver Statistics");
            PdfPTable pt = new PdfPTable(3);
            pt.setWidthPercentage(100);
            addTableHeader(pt, "Driver Name", "Successful Deliveries", "Cancelled");
            boolean alt = false;

            Set<String> allDrivers = new HashSet<>();
            allDrivers.addAll(driverDeliveries.keySet());
            allDrivers.addAll(driverCancellations.keySet());

            if (allDrivers.isEmpty()) {
                addEmptyRow(pt, 3);
            } else {
                for (String d : allDrivers) {
                    long succ = driverDeliveries.getOrDefault(d, 0L);
                    long fail = driverCancellations.getOrDefault(d, 0L);
                    addTableRow(pt, alt, d, fmtNum(succ), fmtNum(fail));
                    alt = !alt;
                }
            }
            doc.add(pt);

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new RuntimeException("Error generating Delivery Performance Report", e);
        }
    }

    /**
     * Generates a Reservation Report in PDF format.
     * Calculates the total reservations, guest turnout, net deposit revenue,
     * and provides a status breakdown (e.g. COMPLETED vs CANCELLED vs NO_SHOW).
     * 
     * @param branchId ID of the branch
     * @param userId ID of the requesting user
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return PDF byte array
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] generateReservationReport(Long branchId, Long userId, LocalDate startDate, LocalDate endDate) {
        String branchName = resolveBranchName(branchId);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Reservation> reservations = reservationRepository.findByBranchIdAndReservationTimeBetween(branchId, start, end);

        long totalReservations = reservations.size();
        long totalGuests = 0;
        BigDecimal totalExpectedRevenue = BigDecimal.ZERO;
        BigDecimal totalRefunds = BigDecimal.ZERO;

        Map<ReservationStatus, Long> statusCount = new EnumMap<>(ReservationStatus.class);

        for (Reservation r : reservations) {
            totalGuests += r.getGuestCount() != null ? r.getGuestCount() : 0;
            if (r.getTotalCharge() != null) {
                totalExpectedRevenue = totalExpectedRevenue.add(r.getTotalCharge());
            }
            if (r.getRefundAmount() != null) {
                totalRefunds = totalRefunds.add(r.getRefundAmount());
            }

            statusCount.put(r.getStatus(), statusCount.getOrDefault(r.getStatus(), 0L) + 1);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = createDocument();
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PageFooter());
            doc.open();

            addReportHeader(doc, "Reservation Report", branchName, startDate, endDate);

            addSectionHeading(doc, "Reservation Overview");
            addSummaryBox(doc,
                    new String[] { "Total Reservations", "Total Guests", "Net Deposit Revenue" },
                    new String[] { fmtNum(totalReservations), fmtNum(totalGuests), fmt(totalExpectedRevenue.subtract(totalRefunds)) });

            addSectionHeading(doc, "Status Breakdown");
            PdfPTable pt = new PdfPTable(2);
            pt.setWidthPercentage(100);
            addTableHeader(pt, "Status", "Count");
            boolean alt = false;

            if (statusCount.isEmpty()) {
                addEmptyRow(pt, 2);
            } else {
                for (Map.Entry<ReservationStatus, Long> e : statusCount.entrySet()) {
                    addTableRow(pt, alt, e.getKey().name(), fmtNum(e.getValue()));
                    alt = !alt;
                }
            }
            doc.add(pt);

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new RuntimeException("Error generating Reservation Report", e);
        }
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
