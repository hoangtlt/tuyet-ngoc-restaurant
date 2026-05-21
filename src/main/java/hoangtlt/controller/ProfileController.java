package hoangtlt.controller;

import hoangtlt.entities.User;
import hoangtlt.services.UsersService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UsersService usersService;

    public ProfileController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping
    public String showProfile(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = usersService.findByUsername(username).orElseThrow();
        
        String mode = "STAFF";
        String roleName = user.getRole().getName();
        if (roleName.equalsIgnoreCase("ADMIN") || roleName.equalsIgnoreCase("MANAGER")) {
            mode = "MANAGEMENT";
        }

        model.addAttribute("user", user);
        model.addAttribute("mode", mode);
        model.addAttribute("activePage", "profile");
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            Authentication authentication,
            RedirectAttributes ra) {
        
        try {
            usersService.updateProfile(authentication.getName(), fullName, email, phone, newPassword);
            ra.addFlashAttribute("success", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/profile";
    }
}
