package hoangtlt.controller.management;

import hoangtlt.entities.SaleOrder;
import hoangtlt.repositories.OrderRepository;
import hoangtlt.services.DashboardService;
import hoangtlt.services.ExportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/management/dashboard")
public class ManagementDashboardController {

    private final DashboardService dashboardService;
    private final ExportService exportService;
    private final OrderRepository orderRepository;

    public ManagementDashboardController(DashboardService dashboardService, ExportService exportService, OrderRepository orderRepository) {
        this.dashboardService = dashboardService;
        this.exportService = exportService;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public String showDashboard(
            @RequestParam(name = "period", defaultValue = "DAY") String period,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year,
            Model model) {
        Map<String, Object> stats = dashboardService.getDashboardStats(period, date, month, year);
        model.addAllAttributes(stats);
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "dashboard");
        return "management/dashboard";
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportExcel(
            @RequestParam(name = "type") String type,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "year", required = false) Integer year) throws IOException {
        
        List<SaleOrder> orders;
        String title;
        String filename;
        LocalDate now = LocalDate.now();
        ByteArrayInputStream in;

        if (type.equalsIgnoreCase("MONTH")) {
            int targetMonth = (month != null) ? month : now.getMonthValue();
            int targetYear = (year != null) ? year : now.getYear();
            LocalDateTime start = LocalDate.of(targetYear, targetMonth, 1).atStartOfDay();
            LocalDateTime end = start.plusMonths(1);
            orders = orderRepository.findByStatusAndCreatedAtBetween(SaleOrder.OrderStatus.PAID, start, end);
            title = "BÁO CÁO DOANH THU THÁNG " + targetMonth + "/" + targetYear;
            filename = "doanh_thu_thang_" + targetMonth + "_" + targetYear + ".xlsx";
            in = exportService.exportMonthlySummaryToExcel(orders, targetMonth, targetYear, title);
        } else if (type.equalsIgnoreCase("YEAR")) {
            int targetYear = (year != null) ? year : now.getYear();
            LocalDateTime start = LocalDate.of(targetYear, 1, 1).atStartOfDay();
            LocalDateTime end = start.plusYears(1);
            orders = orderRepository.findByStatusAndCreatedAtBetween(SaleOrder.OrderStatus.PAID, start, end);
            title = "BÁO CÁO DOANH THU NĂM " + targetYear;
            filename = "doanh_thu_nam_" + targetYear + ".xlsx";
            in = exportService.exportYearlySummaryToExcel(orders, targetYear, title);
        } else {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(SaleOrder.OrderStatus.PAID);
            title = "BÁO CÁO TỔNG DOANH THU";
            filename = "tong_doanh_thu.xlsx";
            in = exportService.exportOrdersToExcel(orders, title);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
