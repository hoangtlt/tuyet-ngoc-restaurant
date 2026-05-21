package hoangtlt.controller.management;

import hoangtlt.dto.OrderDetailDTO;
import hoangtlt.entities.OrderItem;
import hoangtlt.entities.SaleOrder;
import hoangtlt.services.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/management/orders")
public class ManagementOrderController {

    private static final Logger log = LoggerFactory.getLogger(ManagementOrderController.class);
    private final OrderService orderService;
    private final hoangtlt.services.PrintService printService;

    public ManagementOrderController(OrderService orderService, hoangtlt.services.PrintService printService) {
        this.orderService = orderService;
        this.printService = printService;
    }

    @GetMapping
    public String listOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "period", defaultValue = "DAY") String period,
            @RequestParam(name = "date", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            Model model) {

        Page<SaleOrder> orderPage = orderService.getPaginatedOrders(page, 10, period, date);

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("period", period);
        model.addAttribute("selectedDate", date != null ? date.toString() : java.time.LocalDate.now().toString());
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "orders");
        return "management/order-history";
    }

    @PostMapping("/delete/{id}") // Change from GET to POST (Fix bug 5.4)
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteOrder(@PathVariable("id") Long id) {
        orderService.deleteOrder(id);
        return "redirect:/management/orders";
    }

    @PostMapping("/reprint/{id}")
    @ResponseBody
    public java.util.Map<String, Object> reprintOrder(@PathVariable("id") Long id) {
        try {
            SaleOrder order = orderService.getOrderById(id).orElseThrow();
            boolean success = printService.printOrder(order);
            if (success) {
                return java.util.Map.of("success", true, "message", "Đã gửi lệnh in thành công!");
            } else {
                return java.util.Map.of("success", false, "message", "Lỗi: Không thể in. Vui lòng kiểm tra máy in!");
            }
        } catch (Exception e) {
            log.error("Lỗi khi in lại hóa đơn: ", e);
            return java.util.Map.of("success", false, "message", "Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public OrderDetailDTO getOrderDetail(@PathVariable("id") Long id) {
        SaleOrder order = orderService.getOrderById(id).orElseThrow();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        List<OrderDetailDTO.OrderItemDTO> itemDtos = order.getItems().stream()
                .map(item -> new OrderDetailDTO.OrderItemDTO(
                        item.getProduct().getName(),
                        item.getProduct().getImageUrl(),
                        item.getVariantName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getSubTotal(),
                        item.getNote()))
                .collect(Collectors.toList());

        java.math.BigDecimal subTotal = order.getItems().stream()
                .map(hoangtlt.entities.OrderItem::getSubTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal taxAmount = java.math.BigDecimal.ZERO;

        if (order.getTotalAmount().compareTo(subTotal) < 0) {
            discountAmount = subTotal.subtract(order.getTotalAmount());
        } else if (order.getTotalAmount().compareTo(subTotal) > 0) {
            taxAmount = order.getTotalAmount().subtract(subTotal);
        }

        return new OrderDetailDTO(
                order.getId(),
                order.getTable() != null ? order.getTable().getTableNumber() : "N/A",
                order.getStaff() != null ? order.getStaff().getFullName() : "Hệ thống",
                order.getStatus().name(),
                order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "N/A",
                subTotal,
                order.getTotalAmount(),
                discountAmount,
                taxAmount,
                order.getCreatedAt().format(formatter),
                itemDtos);
    }
}
