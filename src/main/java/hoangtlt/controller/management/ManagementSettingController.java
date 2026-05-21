package hoangtlt.controller.management;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/management/settings")
public class ManagementSettingController {

    private final String UPLOAD_DIR = "uploads/settings/";

    @GetMapping("/payment")
    public String showPaymentSettings(Model model) {
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "payment-settings");
        return "management/payment-settings";
    }

    @PostMapping("/payment/update-qr")
    public String updateQR(@RequestParam("qrImage") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn một tệp ảnh.");
            return "redirect:/management/settings/payment";
        }

        try {
            // Đảm bảo thư mục tồn tại
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Lưu file với tên cố định để dễ quản lý (hoặc dùng timestamp)
            String fileName = "qr-payment.png";
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            ra.addFlashAttribute("success", "Đã cập nhật mã QR thành công!");
        } catch (IOException e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Lỗi khi lưu ảnh: " + e.getMessage());
        }

        return "redirect:/management/settings/payment";
    }
}
