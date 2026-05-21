package hoangtlt.controller.common;

import hoangtlt.entities.User;
import hoangtlt.services.UsersService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final UsersService usersService;

    public RegistrationController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        // 1. Kiểm tra Username không khoảng trắng và ký tự đặc biệt
        if (user.getUsername() == null || !user.getUsername().matches("^[a-zA-Z0-9._-]+$")) {
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập không được chứa khoảng trắng hoặc ký tự đặc biệt!");
            return "redirect:/register";
        }

        // 2. Kiểm tra Số điện thoại (Định dạng Việt Nam)
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            if (!user.getPhone().matches("^(0|84)[0-9]{9}$")) {
                redirectAttributes.addFlashAttribute("error", "Số điện thoại không đúng định dạng (phải có 10 số)!");
                return "redirect:/register";
            }
        }

        if (usersService.findByUsername(user.getUsername()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập đã tồn tại!");
            return "redirect:/register";
        }
        
        usersService.register(user);
        redirectAttributes.addFlashAttribute("message", "Đăng ký thành công! Vui lòng đợi quản lý phê duyệt tài khoản.");
        return "redirect:/login";
    }
}
