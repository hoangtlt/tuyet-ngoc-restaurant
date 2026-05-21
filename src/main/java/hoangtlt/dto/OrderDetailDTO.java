package hoangtlt.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetailDTO(
        Long id,
        String tableNumber,
        String staffName,
        String status,
        String paymentMethod,
        BigDecimal subTotal,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        String createdAt,
        List<OrderItemDTO> items) {
    public record OrderItemDTO(
            String productName,
            String imageUrl,
            String variantName,
            Integer quantity,
            BigDecimal price,
            BigDecimal subTotal,
            String note) {
    }
}
