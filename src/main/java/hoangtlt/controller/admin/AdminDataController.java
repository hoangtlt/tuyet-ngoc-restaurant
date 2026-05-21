package hoangtlt.controller.admin;

import hoangtlt.services.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/admin/maintenance")
public class AdminDataController {

    private final OrderService orderService;

    public AdminDataController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String showMaintenancePage(Model model) {
        model.addAttribute("mode", "ADMIN_MAIN");
        model.addAttribute("activePage", "maintenance");
        return "admin/maintenance";
    }

    @PostMapping("/purge")
    public String purgeOrders(@RequestParam("type") String type,
                             @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             RedirectAttributes ra) {
        
        LocalDateTime beforeDate;
        String messagePrefix = "Đã xóa lịch sử đơn hàng ";
        
        // Use end of day for consistency with "up to this date" expectations
        switch (type) {
            case "ALL":
                beforeDate = LocalDateTime.now();
                messagePrefix += "toàn bộ đến hiện tại";
                break;
            case "DATE":
                if (date == null) {
                    ra.addFlashAttribute("error", "Vui lòng chọn ngày!");
                    return "redirect:/admin/maintenance";
                }
                // Purge everything BEFORE the start of the next day (i.e., including the selected date)
                beforeDate = date.atTime(23, 59, 59, 999999999);
                messagePrefix += "trước và trong ngày " + date;
                break;
            case "WEEK":
                beforeDate = LocalDateTime.now().minusWeeks(1);
                messagePrefix += "cũ hơn 1 tuần (trước ngày " + beforeDate.toLocalDate() + ")";
                break;
            case "MONTH":
                beforeDate = LocalDateTime.now().minusMonths(1);
                messagePrefix += "cũ hơn 1 tháng (trước ngày " + beforeDate.toLocalDate() + ")";
                break;
            case "YEAR":
                beforeDate = LocalDateTime.now().minusYears(1);
                messagePrefix += "cũ hơn 1 năm (trước ngày " + beforeDate.toLocalDate() + ")";
                break;
            default:
                ra.addFlashAttribute("error", "Loại xóa không hợp lệ!");
                return "redirect:/admin/maintenance";
        }

        orderService.purgeOrders(beforeDate);
        ra.addFlashAttribute("success", messagePrefix + " thành công!");
        return "redirect:/admin/maintenance";
    }
}
