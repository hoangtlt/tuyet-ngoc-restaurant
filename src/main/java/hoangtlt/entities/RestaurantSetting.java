package hoangtlt.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "restaurant_settings")
public class RestaurantSetting {
    @Id
    private Long id = 1L; // Always use ID 1 for global settings

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String restaurantName = "Quán Ăn Gia Đình";

    @Column(columnDefinition = "NVARCHAR(255)")
    private String address = "Tam Bình, Thủ Đức, TP.HCM";

    @Column
    private String phone = "0123.456.789";

    @Column(columnDefinition = "NVARCHAR(255)")
    private String footerMessage = "Cảm ơn Quý khách! Hẹn gặp lại!";

    @Column
    private String selectedPrinterName = "";

    @Column
    private boolean enableVat = false;

    @Column(nullable = true)
    private Double vatPercentage = 10.0;

    @Column(nullable = true)
    private Integer paperSize = 58; // Default 58mm

    @Column(columnDefinition = "BIT DEFAULT 0")
    private Boolean enableDiscount = false;

    @Column(nullable = true)
    private Double discountPercentage = 0.0;

    public RestaurantSetting() {
    }

    public Double getVatPercentage() {
        return vatPercentage != null ? vatPercentage : 10.0;
    }

    public void setVatPercentage(Double vatPercentage) {
        this.vatPercentage = vatPercentage;
    }

    public Integer getPaperSize() {
        return paperSize != null ? paperSize : 58;
    }

    public void setPaperSize(Integer paperSize) {
        this.paperSize = paperSize;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnableVat() {
        return enableVat;
    }

    public void setEnableVat(boolean enableVat) {
        this.enableVat = enableVat;
    }

    public String getSelectedPrinterName() {
        return selectedPrinterName;
    }

    public void setSelectedPrinterName(String selectedPrinterName) {
        this.selectedPrinterName = selectedPrinterName;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFooterMessage() {
        return footerMessage;
    }

    public void setFooterMessage(String footerMessage) {
        this.footerMessage = footerMessage;
    }

    public Boolean getEnableDiscount() {
        return enableDiscount != null ? enableDiscount : false;
    }

    public void setEnableDiscount(Boolean enableDiscount) {
        this.enableDiscount = enableDiscount;
    }

    public Double getDiscountPercentage() {
        return discountPercentage != null ? discountPercentage : 0.0;
    }

    public void setDiscountPercentage(Double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }
}
