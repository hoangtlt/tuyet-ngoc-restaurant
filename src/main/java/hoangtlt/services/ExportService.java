package hoangtlt.services;

import hoangtlt.entities.SaleOrder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ExportService {

    public ByteArrayInputStream exportMonthlySummaryToExcel(List<SaleOrder> orders, int month, int year, String title)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo tháng");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setBorderBottom(BorderStyle.THIN);
            centerStyle.setBorderTop(BorderStyle.THIN);
            centerStyle.setBorderLeft(BorderStyle.THIN);
            centerStyle.setBorderRight(BorderStyle.THIN);

            // Group by Day
            Map<Integer, BigDecimal> dailyData = new TreeMap<>();
            LocalDate firstDay = LocalDate.of(year, month, 1);
            int daysInMonth = firstDay.lengthOfMonth();
            for (int i = 1; i <= daysInMonth; i++)
                dailyData.put(i, BigDecimal.ZERO);

            for (SaleOrder order : orders) {
                int day = order.getCreatedAt().getDayOfMonth();
                dailyData.put(day, dailyData.get(day).add(order.getTotalAmount()));
            }

            // Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(createTitleStyle(workbook));

            // Header
            String[] columns = { "Ngày", "Doanh thu", "Ghi chú" };
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int rowIdx = 3;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            for (Map.Entry<Integer, BigDecimal> entry : dailyData.entrySet()) {
                Row row = sheet.createRow(rowIdx++);

                Cell dayCell = row.createCell(0);
                dayCell.setCellValue(entry.getKey() + "/" + month + "/" + year);
                dayCell.setCellStyle(centerStyle);

                Cell revCell = row.createCell(1);
                revCell.setCellValue(entry.getValue().doubleValue());
                revCell.setCellStyle(moneyStyle);

                Cell noteCell = row.createCell(2);
                noteCell.setCellValue(""); // Ghi chú trống
                noteCell.setCellStyle(moneyStyle);

                totalRevenue = totalRevenue.add(entry.getValue());
            }

            // Final Total Row
            Row footerRow = sheet.createRow(rowIdx + 1);
            Cell labelCell = footerRow.createCell(0);
            labelCell.setCellValue("TỔNG CỘNG DOANH THU THÁNG:");
            CellStyle footerStyle = createFooterStyle(workbook);
            labelCell.setCellStyle(footerStyle);

            Cell totalCell = footerRow.createCell(1);
            totalCell.setCellValue(totalRevenue.doubleValue());
            CellStyle totalMoneyStyle = workbook.createCellStyle();
            totalMoneyStyle.cloneStyleFrom(moneyStyle);
            Font footerFont = workbook.createFont();
            footerFont.setBold(true);
            totalMoneyStyle.setFont(footerFont);
            totalCell.setCellStyle(totalMoneyStyle);

            autoSizeColumns(sheet, columns.length);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportYearlySummaryToExcel(List<SaleOrder> orders, int year, String title)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo năm");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setBorderBottom(BorderStyle.THIN);
            centerStyle.setBorderTop(BorderStyle.THIN);
            centerStyle.setBorderLeft(BorderStyle.THIN);
            centerStyle.setBorderRight(BorderStyle.THIN);

            // Group by Month
            Map<Integer, BigDecimal> monthlyData = new TreeMap<>();
            for (int i = 1; i <= 12; i++)
                monthlyData.put(i, BigDecimal.ZERO);

            for (SaleOrder order : orders) {
                int m = order.getCreatedAt().getMonthValue();
                monthlyData.put(m, monthlyData.get(m).add(order.getTotalAmount()));
            }

            // Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(createTitleStyle(workbook));

            // Header
            String[] columns = { "Tháng", "Doanh thu", "Ghi chú" };
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int rowIdx = 3;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            for (Map.Entry<Integer, BigDecimal> entry : monthlyData.entrySet()) {
                Row row = sheet.createRow(rowIdx++);

                Cell monthCell = row.createCell(0);
                monthCell.setCellValue("Tháng " + entry.getKey() + "/" + year);
                monthCell.setCellStyle(centerStyle);

                Cell revCell = row.createCell(1);
                revCell.setCellValue(entry.getValue().doubleValue());
                revCell.setCellStyle(moneyStyle);

                Cell noteCell = row.createCell(2);
                noteCell.setCellValue(""); // Ghi chú trống
                noteCell.setCellStyle(moneyStyle);

                totalRevenue = totalRevenue.add(entry.getValue());
            }

            // Final Total Row
            Row footerRow = sheet.createRow(rowIdx + 1);
            Cell labelCell = footerRow.createCell(0);
            labelCell.setCellValue("TỔNG CỘNG DOANH THU NĂM:");
            CellStyle footerStyle = createFooterStyle(workbook);
            labelCell.setCellStyle(footerStyle);

            Cell totalCell = footerRow.createCell(1);
            totalCell.setCellValue(totalRevenue.doubleValue());
            CellStyle totalMoneyStyle = workbook.createCellStyle();
            totalMoneyStyle.cloneStyleFrom(moneyStyle);
            Font footerFont = workbook.createFont();
            footerFont.setBold(true);
            totalMoneyStyle.setFont(footerFont);
            totalCell.setCellStyle(totalMoneyStyle);

            autoSizeColumns(sheet, columns.length);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportOrdersToExcel(List<SaleOrder> orders, String title) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Doanh thu chi tiết");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(createTitleStyle(workbook));

            String[] columns = { "ID", "Thời gian", "Bàn", "Nhân viên", "Thanh toán", "Tổng tiền (VNĐ)" };
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (SaleOrder order : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(order.getId());
                row.createCell(1).setCellValue(order.getCreatedAt().format(formatter));
                row.createCell(2)
                        .setCellValue(order.getTable() != null ? order.getTable().getTableNumber() : "Mang về");
                row.createCell(3).setCellValue(order.getStaff() != null ? order.getStaff().getFullName() : "-");
                row.createCell(4).setCellValue(
                        order.getPaymentMethod() == SaleOrder.PaymentMethod.CASH ? "Tiền mặt" : "Chuyển khoản");
                Cell amountCell = row.createCell(5);
                amountCell.setCellValue(order.getTotalAmount().doubleValue());
                amountCell.setCellStyle(moneyStyle);
                totalRevenue = totalRevenue.add(order.getTotalAmount());
            }

            Row footerRow = sheet.createRow(rowIdx + 1);
            Cell labelCell = footerRow.createCell(4);
            labelCell.setCellValue("TỔNG CỘNG:");
            labelCell.setCellStyle(createFooterStyle(workbook));

            Cell totalCell = footerRow.createCell(5);
            totalCell.setCellValue(totalRevenue.doubleValue());
            CellStyle totalMoneyStyle = workbook.createCellStyle();
            totalMoneyStyle.cloneStyleFrom(moneyStyle);
            Font footerFont = workbook.createFont();
            footerFont.setBold(true);
            totalMoneyStyle.setFont(footerFont);
            totalCell.setCellStyle(totalMoneyStyle);

            autoSizeColumns(sheet, columns.length);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createMoneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createFooterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
