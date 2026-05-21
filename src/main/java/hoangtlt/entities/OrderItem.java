package hoangtlt.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private SaleOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price; // Snapshot price

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subTotal;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String note;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String variantName;

    public OrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SaleOrder getOrder() { return order; }
    public void setOrder(SaleOrder order) { this.order = order; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal subTotal) { this.subTotal = subTotal; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }

    @Column(nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean isPrinted;

    public boolean isPrinted() { return isPrinted; }
    public void setPrinted(boolean isPrinted) { this.isPrinted = isPrinted; }
}
