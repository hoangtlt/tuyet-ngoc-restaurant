package hoangtlt.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private SaleOrder order;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBeforeDiscount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal finalAmount;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String paymentMethod; // CASH, BANK_TRANSFER, MOMO

    @Column(nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    public Invoice() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SaleOrder getOrder() { return order; }
    public void setOrder(SaleOrder order) { this.order = order; }
    public BigDecimal getTotalBeforeDiscount() { return totalBeforeDiscount; }
    public void setTotalBeforeDiscount(BigDecimal totalBeforeDiscount) { this.totalBeforeDiscount = totalBeforeDiscount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
