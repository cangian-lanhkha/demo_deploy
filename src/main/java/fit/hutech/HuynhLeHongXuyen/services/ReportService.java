package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Invoice;
import fit.hutech.HuynhLeHongXuyen.entities.enums.OrderStatus;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentMethod;
import fit.hutech.HuynhLeHongXuyen.repositories.IInvoiceRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IItemInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final IInvoiceRepository invoiceRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;

    /**
     * Revenue by day for chart (labels + data arrays).
     */
    public Map<String, Object> getRevenueByDay(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        List<Object[]> rows = invoiceRepository.revenueByDay(start, end);

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        for (Object[] row : rows) {
            labels.add(row[0].toString());
            data.add(row[1] != null ? ((Number) row[1]).doubleValue() : 0);
        }
        return Map.of("labels", labels, "data", data);
    }

    /**
     * Revenue by payment method for pie chart.
     */
    public Map<String, Object> getRevenueByPaymentMethod(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        List<Object[]> rows = invoiceRepository.revenueByPaymentMethod(start, end);

        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        for (Object[] row : rows) {
            PaymentMethod pm = (PaymentMethod) row[0];
            labels.add(pm != null ? pm.getDisplayName() : "Không rõ");
            data.add(row[1] != null ? ((Number) row[1]).doubleValue() : 0);
        }
        return Map.of("labels", labels, "data", data);
    }

    /**
     * Order count by status for pie chart.
     */
    public Map<String, Object> getOrderCountByStatus(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        List<Object[]> rows = invoiceRepository.countByStatus(start, end);

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (Object[] row : rows) {
            OrderStatus st = (OrderStatus) row[0];
            labels.add(st != null ? st.getDisplayName() : "Không rõ");
            data.add(row[1] != null ? ((Number) row[1]).longValue() : 0);
        }
        return Map.of("labels", labels, "data", data);
    }

    /**
     * Top 10 best-selling books.
     */
    public List<Map<String, Object>> getTopSellingBooks(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        List<Object[]> rows = itemInvoiceRepository.topSellingBooks(start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(rows.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = rows.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", row[0]);
            item.put("quantity", ((Number) row[1]).longValue());
            item.put("revenue", ((Number) row[2]).doubleValue());
            result.add(item);
        }
        return result;
    }

    /**
     * Top 10 customers by spending.
     */
    public List<Map<String, Object>> getTopCustomers(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        List<Object[]> rows = itemInvoiceRepository.topCustomers(start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(rows.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = rows.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", row[0]);
            item.put("orders", ((Number) row[1]).longValue());
            item.put("totalSpent", ((Number) row[2]).doubleValue());
            result.add(item);
        }
        return result;
    }

    /**
     * Summary statistics for the period.
     */
    public Map<String, Object> getSummary(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        Double revenue = invoiceRepository.totalRevenueBetween(start, end);
        long totalOrders = invoiceRepository.countBetween(start, end);
        long successfulOrders = invoiceRepository.countSuccessfulBetween(start, end);
        double conversionRate = totalOrders > 0 ? (double) successfulOrders / totalOrders * 100 : 0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("revenue", revenue != null ? revenue : 0.0);
        summary.put("totalOrders", totalOrders);
        summary.put("successfulOrders", successfulOrders);
        summary.put("conversionRate", Math.round(conversionRate * 10.0) / 10.0);
        return summary;
    }

    /**
     * Get orders for CSV export.
     */
    public List<Invoice> getOrdersForExport(LocalDate from, LocalDate to) {
        return invoiceRepository.findByOrderDateBetweenOrderByOrderDateDesc(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));
    }
}
