package hoangtlt.controller.common;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping(value = "/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("message", "Bạn không có quyền truy cập vào chức năng này!");
        return "error-page";
    }

}
