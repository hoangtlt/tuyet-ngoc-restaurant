package hoangtlt.controller.admin;

import hoangtlt.entities.RestaurantSetting;
import hoangtlt.services.RestaurantSettingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final RestaurantSettingService settingService;

    public AdminSettingsController(RestaurantSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("settings", settingService.getSettings());
        model.addAttribute("mode", "ADMIN_MAIN");
        model.addAttribute("activePage", "settings");
        return "admin/settings";
    }

    @PostMapping("/save")
    public String saveSettings(RestaurantSetting settings, RedirectAttributes redirectAttributes) {
        settingService.saveSettings(settings); // This service already handles ID=1 logic
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật thông tin nhà hàng thành công!");
        return "redirect:/admin/settings";
    }
}
