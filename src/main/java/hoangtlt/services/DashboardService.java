package hoangtlt.services;

import hoangtlt.entities.OrderItem;
import hoangtlt.entities.SaleOrder;
import hoangtlt.repositories.OrderRepository;
import hoangtlt.config.AppConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final OrderRepository orderRepository;

    public DashboardService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> getDashboardStats(String period, LocalDate date, Integer month, Integer year) {
        final String effectivePeriod = (period == null || period.isEmpty()) ? "DAY" : period.toUpperCase();
        Map<String, Object> stats = new HashMap<>();
        stats.put("currentPeriod", effectivePeriod);

        LocalDate now = LocalDate.now();
        LocalDate targetDate = (date != null) ? date : now;
        int targetYear = (year != null) ? year : now.getYear();
        int targetMonth = (month != null) ? month : now.getMonthValue();

        stats.put("selectedDate", targetDate.toString());
        stats.put("selectedMonth", targetMonth);
        stats.put("selectedYear", targetYear);

        LocalDateTime todayStart = targetDate.atStartOfDay();
        LocalDateTime tomorrowStart = targetDate.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = targetDate.minusDays(1).atStartOfDay();

        // Luôn tính doanh thu hôm nay và hôm qua để hiện ở widget
        BigDecimal sumToday = orderRepository.sumTotalAmountByStatusAndCreatedAtBetween(SaleOrder.OrderStatus.PAID,
                todayStart, tomorrowStart);
        BigDecimal sumYesterday = orderRepository.sumTotalAmountByStatusAndCreatedAtBetween(SaleOrder.OrderStatus.PAID,
                yesterdayStart, todayStart);

        stats.put("revenueToday", sumToday != null ? sumToday : BigDecimal.ZERO);
        stats.put("revenueYesterday", sumYesterday != null ? sumYesterday : BigDecimal.ZERO);

        // Xử lý thống kê theo kỳ (Period)
        LocalDateTime start;
        LocalDateTime end = tomorrowStart; // Mặc định là đến hết hôm nay
        Map<String, BigDecimal> chartData = new LinkedHashMap<>();
        String chartLabel = "";

        switch (effectivePeriod) {
            case "WEEK":
                start = targetDate.minusDays(6).atStartOfDay();
                end = tomorrowStart;
                chartLabel = "Doanh thu 7 ngày (" + targetDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) + " trở về trước)";
                for (int i = 6; i >= 0; i--)
                    chartData.put(targetDate.minusDays(i).toString(), BigDecimal.ZERO);
                break;
            case "MONTH":
                if (month != null && year != null) {
                    LocalDate firstDay = LocalDate.of(year, month, 1);
                    start = firstDay.atStartOfDay();
                    end = firstDay.plusMonths(1).atStartOfDay();
                    chartLabel = "Doanh thu tháng " + month + "/" + year;
                    for (int i = 1; i <= firstDay.lengthOfMonth(); i++) {
                        chartData.put(firstDay.withDayOfMonth(i).toString(), BigDecimal.ZERO);
                    }
                } else {
                    start = targetDate.minusDays(29).atStartOfDay();
                    end = tomorrowStart;
                    chartLabel = "Doanh thu 30 ngày (" + targetDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) + " trở về trước)";
                    for (int i = 29; i >= 0; i--)
                        chartData.put(targetDate.minusDays(i).toString(), BigDecimal.ZERO);
                }
                break;
            case "YEAR":
                start = LocalDate.of(targetYear, 1, 1).atStartOfDay();
                end = LocalDate.of(targetYear + 1, 1, 1).atStartOfDay();
                chartLabel = "Doanh thu năm " + targetYear;
                for (int i = 1; i <= 12; i++)
                    chartData.put("Th" + i, BigDecimal.ZERO);
                break;
            case "DAY":
            default:
                start = todayStart;
                end = tomorrowStart;
                if (date != null) {
                    chartLabel = "Doanh thu theo giờ (" + targetDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
                } else {
                    chartLabel = "Doanh thu theo giờ (Hôm nay)";
                }
                for (int i = 0; i < 24; i++)
                    chartData.put(i + "h", BigDecimal.ZERO);
                break;
        }

        List<SaleOrder> periodOrders = orderRepository.findByStatusAndCreatedAtBetween(SaleOrder.OrderStatus.PAID,
                start, end);

        periodOrders.forEach(o -> {
            String key;
            if (effectivePeriod.equalsIgnoreCase("DAY")) {
                key = o.getCreatedAt().getHour() + "h";
            } else if (effectivePeriod.equalsIgnoreCase("YEAR")) {
                key = "Th" + o.getCreatedAt().getMonthValue();
            } else {
                key = o.getCreatedAt().toLocalDate().toString();
            }
            if (chartData.containsKey(key)) {
                chartData.put(key, chartData.get(key).add(o.getTotalAmount()));
            }
        });

        BigDecimal revenuePeriod = periodOrders.stream().map(SaleOrder::getTotalAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        long orderCount = periodOrders.size();

        stats.put("revenuePeriod", revenuePeriod);
        stats.put("orderCountPeriod", orderCount);
        stats.put("aovPeriod",
                orderCount > 0 ? revenuePeriod.divide(BigDecimal.valueOf(orderCount), 0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);

        stats.put("chartLabels", new ArrayList<>(chartData.keySet()));
        stats.put("chartValues", new ArrayList<>(chartData.values()));
        stats.put("chartTitle", chartLabel);

        // Best Sellers
        Map<String, Integer> productSales = new HashMap<>();
        periodOrders.forEach(o -> {
            o.getItems().forEach(item -> {
                String name = item.getProduct().getName();
                productSales.put(name, productSales.getOrDefault(name, 0) + item.getQuantity());
            });
        });
        stats.put("topProducts", productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5)
                .collect(Collectors.toList()));

        // Service Stats (Fixing N+1 with direct DB count)
        stats.put("dineInCount", orderCount - orderRepository.countByAreaNameAndStatusAndCreatedAtBetween(
                AppConstants.TAKEAWAY_AREA_NAME, SaleOrder.OrderStatus.PAID, start, end));
        stats.put("takeAwayCount", orderRepository.countByAreaNameAndStatusAndCreatedAtBetween(
                AppConstants.TAKEAWAY_AREA_NAME, SaleOrder.OrderStatus.PAID, start, end));

        // Payment Methods
        stats.put("cashCount",
                periodOrders.stream().filter(o -> o.getPaymentMethod() == SaleOrder.PaymentMethod.CASH).count());
        stats.put("bankCount",
                periodOrders.stream().filter(o -> o.getPaymentMethod() == SaleOrder.PaymentMethod.TRANSFER).count());

        // Unfinished (Fixing N+1/Memory with direct count)
        stats.put("pendingOrders", orderRepository.countPendingOrders());

        return stats;
    }
}
