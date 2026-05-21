package hoangtlt.controller.management;

import hoangtlt.entities.RestaurantSetting;
import hoangtlt.services.PrintService;
import hoangtlt.services.RestaurantSettingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/management/printer")
public class ManagementPrinterController {

    private final PrintService printService;
    private final RestaurantSettingService settingsService;

    public ManagementPrinterController(PrintService printService, RestaurantSettingService settingsService) {
        this.printService = printService;
        this.settingsService = settingsService;
    }

    @GetMapping
    public String showPrinterSettings(Model model) {
        model.addAttribute("printers", printService.getInstalledPrinters());
        model.addAttribute("selectedPrinter", printService.getSelectedPrinterName());
        model.addAttribute("billSettings", settingsService.getSettings());
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "printer");
        return "management/printer-settings";
    }

    @PostMapping("/save")
    public String savePrinter(@RequestParam("printerName") String printerName, 
                             @RequestParam("paperSize") int paperSize,
                             RedirectAttributes ra) {
        RestaurantSetting settings = settingsService.getSettings();
        settings.setSelectedPrinterName(printerName);
        settings.setPaperSize(paperSize);
        settingsService.saveSettings(settings);
        
        ra.addFlashAttribute("success", "Đã lưu cấu hình máy in (" + paperSize + "mm): " + printerName);
        return "redirect:/management/printer";
    }

    @PostMapping("/bill-settings")
    public String saveBillSettings(@ModelAttribute RestaurantSetting settings, RedirectAttributes ra) {
        settingsService.saveSettings(settings);
        ra.addFlashAttribute("success", "Đã cập nhật thông tin hóa đơn thành công!");
        return "redirect:/management/printer";
    }

    @GetMapping("/test")
    public String testPrint(RedirectAttributes ra) {
        boolean result = printService.printTestPage();
        if (result) {
            ra.addFlashAttribute("success", "Đã gửi lệnh in thử tới máy in.");
        } else {
            ra.addFlashAttribute("error", "Lỗi: Chưa cấu hình máy in hoặc máy in không hoạt động.");
        }
        return "redirect:/management/printer";
    }
}
