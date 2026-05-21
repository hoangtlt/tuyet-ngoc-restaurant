package hoangtlt.controller.staff;

import hoangtlt.entities.*;
import hoangtlt.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/order")
public class StaffOrderController {

    private static final Logger log = LoggerFactory.getLogger(StaffOrderController.class);
    private final OrderService orderService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final PrintService printService;
    private final RestaurantSettingService settingsService;

    public StaffOrderController(OrderService orderService, ProductService productService,
            CategoryService categoryService, PrintService printService,
            RestaurantSettingService settingsService) {
        this.orderService = orderService;
        this.productService = productService;
        this.categoryService = categoryService;
        this.printService = printService;
        this.settingsService = settingsService;
    }

    @GetMapping("/takeaway")
    public String takeaway() {
        return "redirect:/order/table/" + orderService.getOrCreateTakeawayTable().getId();
    }

    @GetMapping("/table/{tableId}")
    public String showOrderPage(@PathVariable("tableId") Long tableId, Model model, Authentication authentication) {
        SaleOrder order = orderService.getActiveOrder(tableId).orElse(null);
        if (order != null && order.getItems() != null) {
            order.getItems().sort((a, b) -> {
                boolean aIsDrink = (a.getProduct().getCategory().getId() == 10);
                boolean bIsDrink = (b.getProduct().getCategory().getId() == 10);
                if (aIsDrink && !bIsDrink)
                    return 1;
                if (!aIsDrink && bIsDrink)
                    return -1;

                if (a.isPrinted() && !b.isPrinted())
                    return 1;
                if (!a.isPrinted() && b.isPrinted())
                    return -1;

                return b.getId().compareTo(a.getId());
            });
        }
        model.addAttribute("order", order);
        model.addAttribute("table", orderService.getTable(tableId));
        model.addAttribute("currentStaff", orderService.getUserByUsername(authentication.getName()));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("billSettings", settingsService.getSettings());
        model.addAttribute("mode", "STAFF");
        return "staff/order";
    }

    @PostMapping("/add-item/{tableId}")
    public String addItem(@PathVariable("tableId") Long tableId,
            @RequestParam(value = "orderId", required = false) Long orderId,
            @RequestParam("productId") Long productId,
            @RequestParam(value = "variantId", required = false) Long variantId,
            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
            @RequestParam(value = "note", required = false) String note,
            Authentication authentication) {

        // Validation (Fix bug 4.4 - although it's about upload, adding general
        // validation)
        if (quantity < 1) {
            return "redirect:/order/table/" + tableId;
        }

        // Lazy create order
        SaleOrder order = orderService.getOrCreateActiveOrder(tableId, authentication.getName());
        orderService.addProductToOrder(order.getId(), productId, variantId, quantity, note);
        return "redirect:/order/table/" + tableId;
    }

    @PostMapping("/remove-item/{tableId}/{orderId}/{orderItemId}")
    public String removeItem(@PathVariable("tableId") Long tableId, @PathVariable("orderId") Long orderId,
            @PathVariable("orderItemId") Long orderItemId) {
        orderService.removeProductFromOrder(orderId, orderItemId);
        return "redirect:/order/table/" + tableId;
    }

    @PostMapping("/update-qty/{tableId}/{orderId}/{orderItemId}/{delta}")
    public String updateQty(@PathVariable("tableId") Long tableId,
            @PathVariable("orderId") Long orderId,
            @PathVariable("orderItemId") Long orderItemId,
            @PathVariable("delta") int delta) {
        orderService.updateItemQuantity(orderId, orderItemId, delta);
        return "redirect:/order/table/" + tableId;
    }

    @GetMapping("/back/{tableId}/{orderId}")
    public String back(@PathVariable("tableId") Long tableId, @PathVariable("orderId") Long orderId) {
        orderService.deleteOrderIfEmpty(orderId);
        return "redirect:/";
    }

    @PostMapping("/update-note/{tableId}")
    public String updateNote(@PathVariable("tableId") Long tableId,
            @RequestParam("orderId") Long orderId,
            @RequestParam("productId") Long productId,
            @RequestParam(value = "variantName", required = false) String variantName,
            @RequestParam("note") String note) {
        orderService.updateItemNote(orderId, productId, variantName, note);
        return "redirect:/order/table/" + tableId;
    }

    @PostMapping("/clear-table/{tableId}")
    public String clearTable(@PathVariable("tableId") Long tableId) {
        orderService.clearTable(tableId);
        return "redirect:/";
    }

    @PostMapping("/print-kitchen/{orderId}")
    @ResponseBody
    public ResponseEntity<?> printKitchenOrder(@PathVariable("orderId") Long orderId) {
        try {
            SaleOrder order = orderService.getOrderById(orderId).orElseThrow();

            boolean hasPrintedItems = order.getItems().stream().anyMatch(hoangtlt.entities.OrderItem::isPrinted);

            java.util.List<hoangtlt.entities.OrderItem> unprintedItems = order.getItems().stream()
                    .filter(item -> !item.isPrinted())
                    .collect(java.util.stream.Collectors.toList());

            if (unprintedItems.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Không có món mới nào để báo bếp!"));
            }

            boolean success = printService.printKitchenItems(order, unprintedItems, hasPrintedItems);
            if (success) {
                orderService.markItemsAsPrinted(orderId, unprintedItems);
                return ResponseEntity.ok(Map.of("message", "Đã gửi lệnh báo bếp thành công!"));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Không thể in báo bếp. Vui lòng kiểm tra cấu hình máy in!"));
            }
        } catch (Exception e) {
            log.error("Lỗi in bếp: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/checkout/{tableId}")
    public String checkout(@PathVariable("tableId") Long tableId,
            @RequestParam("orderId") Long orderId,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "printBill", required = false) String printBill,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        SaleOrder order = orderService.getOrderById(orderId).orElseThrow();

        // Guard check: Chặn nếu không phải trạng thái PENDING (Fix bug 3.3)
        if (order.getStatus() != SaleOrder.OrderStatus.PENDING) {
            log.warn("Cố gắng thanh toán đơn hàng đã xử lý: {}", orderId);
            return "redirect:/order/table/" + tableId;
        }

        // Chặn thanh toán nếu đơn hàng trống
        if (order.getItems() == null || order.getItems().isEmpty()) {
            ra.addFlashAttribute("error", "Không thể thanh toán đơn hàng trống!");
            return "redirect:/order/table/" + tableId;
        }

        try {
            SaleOrder.PaymentMethod method = SaleOrder.PaymentMethod.valueOf(paymentMethod);
            orderService.checkoutOrder(orderId, method);

            if ("true".equals(printBill)) {
                boolean printSuccess = printService.printOrder(order);
                if (printSuccess) {
                    ra.addFlashAttribute("success", "Thanh toán thành công và đã in hóa đơn.");
                } else {
                    ra.addFlashAttribute("warning",
                            "Thanh toán thành công nhưng máy in lỗi. Vui lòng kiểm tra máy in!");
                }
            } else {
                ra.addFlashAttribute("success", "Thanh toán thành công.");
            }
        } catch (IllegalArgumentException e) {
            log.error("Phương thức thanh toán không hợp lệ: {}", paymentMethod);
            ra.addFlashAttribute("error", "Phương thức thanh toán không hợp lệ!");
            return "redirect:/order/table/" + tableId;
        } catch (Exception e) {
            log.error("Lỗi khi thanh toán đơn hàng {}: {}", orderId, e.getMessage(), e);
            ra.addFlashAttribute("error", "Lỗi hệ thống khi thanh toán: " + e.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/history")
    public String orderHistory(Model model, Authentication authentication,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        User staff = orderService.getUserByUsername(authentication.getName());
        if (staff == null) {
            return "redirect:/login";
        }
        model.addAttribute("orders", orderService.getStaffOrderHistory(staff.getId(), page, size));
        model.addAttribute("currentStaff", staff);

        // Determine sidebar mode
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            model.addAttribute("mode", "ADMIN_MAIN");
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER"))) {
            model.addAttribute("mode", "MANAGEMENT");
        } else {
            model.addAttribute("mode", "STAFF");
        }

        return "staff/history";
    }

    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public hoangtlt.dto.OrderDetailDTO getOrderDetail(@PathVariable("id") Long id) {
        SaleOrder order = orderService.getOrderById(id).orElseThrow();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        java.util.List<hoangtlt.dto.OrderDetailDTO.OrderItemDTO> itemDtos = order.getItems().stream()
                .map(item -> new hoangtlt.dto.OrderDetailDTO.OrderItemDTO(
                        item.getProduct().getName(),
                        item.getProduct().getImageUrl(),
                        item.getVariantName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getSubTotal(),
                        item.getNote()))
                .collect(java.util.stream.Collectors.toList());

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

        return new hoangtlt.dto.OrderDetailDTO(
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
}
