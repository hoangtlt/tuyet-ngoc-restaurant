package hoangtlt.services;

import hoangtlt.entities.OrderItem;
import hoangtlt.entities.RestaurantSetting;
import hoangtlt.entities.SaleOrder;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.print.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrintService {

    private final DecimalFormat df = new DecimalFormat("#,###");
    private final RestaurantSettingService settingService;

    public PrintService(RestaurantSettingService settingService) {
        this.settingService = settingService;
    }

    public List<String> getInstalledPrinters() {
        List<String> printerNames = new ArrayList<>();
        javax.print.PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        for (javax.print.PrintService printer : printServices) {
            printerNames.add(printer.getName());
        }
        return printerNames;
    }

    public String getSelectedPrinterName() {
        return settingService.getSettings().getSelectedPrinterName();
    }

    public void setSelectedPrinterName(String selectedPrinterName) {
        RestaurantSetting settings = settingService.getSettings();
        settings.setSelectedPrinterName(selectedPrinterName);
        settingService.saveSettings(settings);
    }

    public boolean printOrder(SaleOrder order) {
        return printOrder(order, false);
    }

    public boolean printKitchenOrder(SaleOrder order) {
        return printOrder(order, true); 
    }

    public boolean printKitchenItems(SaleOrder order, List<OrderItem> itemsToPrint, boolean isSupplement) {
        String printerName = getSelectedPrinterName();
        if (printerName == null || printerName.isEmpty())
            return false;

        PrinterJob job = PrinterJob.getPrinterJob();
        javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        javax.print.PrintService selectedService = null;

        for (javax.print.PrintService service : services) {
            if (service.getName().equals(printerName)) {
                selectedService = service;
                break;
            }
        }

        if (selectedService == null)
            return false;

        try {
            job.setPrintService(selectedService);
            PageFormat pf = job.defaultPage();
            Paper paper = pf.getPaper();
            RestaurantSetting settings = settingService.getSettings();
            int paperSizeMm = settings.getPaperSize();

            double fullWidth = paperSizeMm * 2.8346;
            double printableWidth = fullWidth - 12;
            double margin = (fullWidth - printableWidth) / 2;

            paper.setSize(fullWidth, 1000);
            paper.setImageableArea(margin, 0, printableWidth, 1000);
            pf.setPaper(paper);

            job.setPrintable(new OrderPrintable(order, itemsToPrint, true, isSupplement), pf);
            job.print();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean printOrder(SaleOrder order, boolean isKitchen) {
        String printerName = getSelectedPrinterName();
        if (printerName == null || printerName.isEmpty())
            return false;

        PrinterJob job = PrinterJob.getPrinterJob();
        javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        javax.print.PrintService selectedService = null;

        for (javax.print.PrintService service : services) {
            if (service.getName().equals(printerName)) {
                selectedService = service;
                break;
            }
        }

        if (selectedService == null)
            return false;

        try {
            job.setPrintService(selectedService);

            PageFormat pf = job.defaultPage();
            Paper paper = pf.getPaper();

            RestaurantSetting settings = settingService.getSettings();
            int paperSizeMm = settings.getPaperSize(); // 58 or 80

            double fullWidth = paperSizeMm * 2.8346;
            // Cho phép lề 2mm mỗi bên (2 * 2.8346 = 5.6 pts)
            double printableWidth = fullWidth - 12;
            double margin = (fullWidth - printableWidth) / 2;

            paper.setSize(fullWidth, 1000);
            paper.setImageableArea(margin, 0, printableWidth, 1000);
            pf.setPaper(paper);

            job.setPrintable(new OrderPrintable(order, isKitchen), pf);
            job.print();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private class OrderPrintable implements Printable {
        private final SaleOrder order;
        private final boolean isKitchen;
        private final List<OrderItem> specificItems;
        private final boolean isSupplement;

        public OrderPrintable(SaleOrder order, boolean isKitchen) {
            this.order = order;
            this.isKitchen = isKitchen;
            this.specificItems = null;
            this.isSupplement = false;
        }

        public OrderPrintable(SaleOrder order, List<OrderItem> specificItems, boolean isKitchen, boolean isSupplement) {
            this.order = order;
            this.isKitchen = isKitchen;
            this.specificItems = specificItems;
            this.isSupplement = isSupplement;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0)
                return NO_SUCH_PAGE;

            RestaurantSetting settings = settingService.getSettings();
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2d.setColor(Color.BLACK);

            int y = 15;
            int width = (int) pageFormat.getImageableWidth();

            if (!isKitchen) {
                g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
                drawCenteredString(g2d, settings.getRestaurantName(), y, width);
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 8));
                y += 12;
                drawCenteredString(g2d, settings.getAddress(), y, width);
                y += 10;
                drawCenteredString(g2d, "SĐT: " + settings.getPhone(), y, width);
                y += 15;
                
                g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
                if (order.getStatus() == hoangtlt.entities.SaleOrder.OrderStatus.PAID) {
                    drawCenteredString(g2d, "HÓA ĐƠN THANH TOÁN", y, width);
                    y += 12;
                    g2d.setFont(new Font("SansSerif", Font.ITALIC, 9));
                    drawCenteredString(g2d, "(Liên lưu quán)", y, width);
                } else {
                    drawCenteredString(g2d, "PHIẾU TẠM TÍNH", y, width);
                }
                y += 10;
            } else {
                g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
                String title = isSupplement ? "MÓN BỔ SUNG" : "PHIẾU BÁO BẾP";
                drawCenteredString(g2d, title, y, width);
                y += 15;
            }

            g2d.drawLine(0, y, width, y);
            y += 15;

            g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2d.drawString("Bàn: " + order.getTable().getTableNumber(), 0, y);

            y += 14;
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yy");
            g2d.drawString("Giờ: " + order.getCreatedAt().format(dtf), 0, y);
            y += 12;
            g2d.drawString("NV:  " + order.getStaff().getFullName(), 0, y);

            y += 8;
            g2d.drawLine(0, y, width, y);

            // Table Header
            y += 12;
            g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2d.drawString("Tên món", 0, y);
            if (!isKitchen) {
                y += 12;
                int colPriceRight = width - (settings.getPaperSize() == 80 ? 110 : 90);
                int colQty = width - (settings.getPaperSize() == 80 ? 65 : 55);
                drawRightString(g2d, "Đ.Giá", y, colPriceRight);
                g2d.drawString("SL", colQty, y);
                drawRightString(g2d, "T.Tiền", y, width);
            } else {
                y += 12;
                drawRightString(g2d, "Đ.Giá", y, width - (settings.getPaperSize() == 80 ? 80 : 60));
                drawRightString(g2d, "SL", y, width);
            }

            y += 5;
            g2d.drawLine(0, y, width, y);

            y += 14;
            // Xử lý danh sách món hiển thị
            List<OrderItem> displayItems = new ArrayList<>();
            if (!isKitchen) {
                // Gộp các món giống nhau trên Bill thanh toán
                java.util.Map<String, OrderItem> grouped = new java.util.LinkedHashMap<>();
                for (OrderItem item : order.getItems()) {
                    String key = item.getProduct().getId() + "_" +
                            (item.getVariantName() != null ? item.getVariantName() : "") + "_" +
                            item.getPrice().toString();
                    if (grouped.containsKey(key)) {
                        OrderItem existing = grouped.get(key);
                        existing.setQuantity(existing.getQuantity() + item.getQuantity());
                        existing.setSubTotal(existing.getSubTotal().add(item.getSubTotal()));
                        if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
                            if (existing.getNote() == null || existing.getNote().trim().isEmpty()) {
                                existing.setNote(item.getNote());
                            } else {
                                existing.setNote(existing.getNote() + " | " + item.getNote());
                            }
                        }
                    } else {
                        OrderItem copy = new OrderItem();
                        copy.setProduct(item.getProduct());
                        copy.setVariantName(item.getVariantName());
                        copy.setPrice(item.getPrice());
                        copy.setQuantity(item.getQuantity());
                        copy.setSubTotal(item.getSubTotal());
                        copy.setNote(item.getNote());
                        grouped.put(key, copy);
                    }
                }
                displayItems.addAll(grouped.values());
            } else {
                if (specificItems != null) {
                    displayItems.addAll(specificItems);
                } else {
                    displayItems.addAll(order.getItems());
                }
            }

            displayItems.sort((a, b) -> {
                boolean aIsDrink = (a.getProduct().getCategory().getId() == 10);
                boolean bIsDrink = (b.getProduct().getCategory().getId() == 10);
                if (aIsDrink && !bIsDrink) return 1;
                if (!aIsDrink && bIsDrink) return -1;

                int prodCmp = a.getProduct().getName().compareToIgnoreCase(b.getProduct().getName());
                if (prodCmp != 0)
                    return prodCmp;
                String v1 = a.getVariantName() != null ? a.getVariantName() : "";
                String v2 = b.getVariantName() != null ? b.getVariantName() : "";
                return v1.compareToIgnoreCase(v2);
            });

            for (int i = 0; i < displayItems.size(); i++) {
                OrderItem item = displayItems.get(i);
                String name = item.getProduct().getName();
                if (item.getVariantName() != null) {
                    name += " (" + item.getVariantName() + ")";
                }

                if (isKitchen) {
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
                } else {
                    g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
                }

                // Vẽ tên món (có thể bị dài nên có thể cần giới hạn hoặc vẽ khéo)
                g2d.drawString(name, 0, y);

                if (!isKitchen) {
                    int colPriceRight = width - (settings.getPaperSize() == 80 ? 110 : 90);
                    int colQty = width - (settings.getPaperSize() == 80 ? 65 : 55);
                    y += 12;
                    g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    drawRightString(g2d, df.format(item.getPrice()), y, colPriceRight);
                    g2d.drawString("x" + item.getQuantity(), colQty, y);
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 9));
                    drawRightString(g2d, df.format(item.getSubTotal()), y, width);
                } else {
                    y += 12;
                    g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    drawRightString(g2d, df.format(item.getPrice()), y, width - (settings.getPaperSize() == 80 ? 80 : 60));
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
                    drawRightString(g2d, "x" + item.getQuantity(), y, width);
                }

                y += 12;

                // In ghi chú cho cả Bếp và Bill
                if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
                    g2d.setFont(new Font("SansSerif", Font.ITALIC, 8));
                    g2d.drawString("Note: " + item.getNote(), 5, y);
                    y += 10;
                }

                if (i < displayItems.size() - 1) {
                    g2d.setColor(Color.GRAY);
                    // Tạo đường kẻ đứt khúc (dash) mờ bằng Stroke
                    Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
                            new float[] { 2 }, 0);
                    Stroke oldStroke = g2d.getStroke();
                    g2d.setStroke(dashed);

                    g2d.drawLine(0, y, width, y);

                    g2d.setStroke(oldStroke);
                    g2d.setColor(Color.BLACK);

                    y += 14; // Tăng khoảng cách từ gạch tới món tiếp theo
                }
            }

            y += 5;
            g2d.drawLine(0, y, width, y);

            if (!isKitchen) {
                y += 15;
                BigDecimal subtotal = order.getItems().stream()
                        .map(OrderItem::getSubTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (settings.getEnableDiscount() != null && settings.getEnableDiscount() && settings.getDiscountPercentage() > 0) {
                    g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    g2d.drawString("Tạm tính:", 0, y);
                    drawRightString(g2d, df.format(subtotal), y, width);
                    y += 12;
                    BigDecimal discountRate = BigDecimal.valueOf(settings.getDiscountPercentage());
                    BigDecimal discountAmount = subtotal.multiply(discountRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    BigDecimal grandTotal = subtotal.subtract(discountAmount);
                    g2d.drawString("Giảm giá (" + settings.getDiscountPercentage() + "%):", 0, y);
                    drawRightString(g2d, "- " + df.format(discountAmount), y, width);
                    y += 15;
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2d.drawString("TỔNG CỘNG:", 0, y);
                    drawRightString(g2d, df.format(grandTotal) + "đ", y, width);
                } else if (settings.isEnableVat() && settings.getVatPercentage() > 0) {
                    g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    g2d.drawString("Tạm tính:", 0, y);
                    drawRightString(g2d, df.format(subtotal), y, width);
                    y += 12;
                    BigDecimal vatRate = BigDecimal.valueOf(settings.getVatPercentage());
                    BigDecimal taxAmount = subtotal.multiply(vatRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    BigDecimal grandTotal = subtotal.add(taxAmount);
                    g2d.drawString("Thuế VAT (" + settings.getVatPercentage() + "%):", 0, y);
                    drawRightString(g2d, df.format(taxAmount), y, width);
                    y += 15;
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2d.drawString("TỔNG CỘNG:", 0, y);
                    drawRightString(g2d, df.format(grandTotal) + "đ", y, width);
                } else {
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2d.drawString("TỔNG CỘNG:", 0, y);
                    drawRightString(g2d, df.format(subtotal) + "đ", y, width);
                }
                y += 20;

                // Thêm mã QR thanh toán hoặc đánh dấu Đã thanh toán
                if (order.getStatus() == hoangtlt.entities.SaleOrder.OrderStatus.PENDING) {
                    try {
                        File qrFile = new File("uploads/settings/qr-payment.png");
                        if (qrFile.exists()) {
                            BufferedImage qrImage = ImageIO.read(qrFile);
                            if (qrImage != null) {
                                int qrSize = (int) (width * 0.85); // Kích thước in QR (85% chiều rộng giấy)
                                int qrX = (width - qrSize) / 2;
                                g2d.drawImage(qrImage, qrX, y, qrSize, qrSize, null);
                                y += qrSize + 15;
                                
                                g2d.setFont(new Font("SansSerif", Font.BOLD, 9));
                                drawCenteredString(g2d, "Quét mã để thanh toán", y, width);
                                y += 15;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Không thể in mã QR: " + e.getMessage());
                    }
                } else {
                    y += 15;
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
                    drawCenteredString(g2d, "*** ĐÃ THANH TOÁN ***", y, width);
                    y += 15;
                }

                g2d.setFont(new Font("SansSerif", Font.ITALIC, 8));
                drawCenteredString(g2d, settings.getFooterMessage(), y, width);
            } else {
                y += 20;
                g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
                drawCenteredString(g2d, "*** BẾP CHẾ BIẾN ***", y, width);
            }

            y += 40;
            g2d.drawString(".", 0, y);
            return PAGE_EXISTS;
        }

        private void drawCenteredString(Graphics2D g, String text, int y, int width) {
            FontMetrics metrics = g.getFontMetrics();
            int x = (width - metrics.stringWidth(text)) / 2;
            g.drawString(text, x, y);
        }

        private void drawRightString(Graphics2D g, String text, int y, int width) {
            FontMetrics metrics = g.getFontMetrics();
            int x = width - metrics.stringWidth(text);
            g.drawString(text, x, y);
        }
    }

    public boolean printTestPage() {
        return true;
    }
}
