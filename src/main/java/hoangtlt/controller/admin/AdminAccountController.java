package hoangtlt.controller.admin;

import hoangtlt.entities.User;
import hoangtlt.repositories.RoleRepository;
import hoangtlt.services.AdminUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
public class AdminAccountController {

    private final AdminUserService userService;
    private final RoleRepository roleRepository;

    public AdminAccountController(AdminUserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("mode", "ADMIN_MAIN");
        model.addAttribute("isPendingPage", false);
        return "admin/user-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("title", "Thêm tài khoản");
        model.addAttribute("mode", "ADMIN_MAIN");
        return "admin/user-form";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute("user") User user) {
        userService.saveUser(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("title", "Chỉnh sửa tài khoản");
        model.addAttribute("mode", "ADMIN_MAIN");
        return "admin/user-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài khoản thành công!");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            userService.lockUser(id);
            redirectAttributes.addFlashAttribute("warningMessage", "Tài khoản này đã có hóa đơn liên quan nên không thể xóa. Hệ thống đã tự động chuyển sang trạng thái 'Bị khóa'!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/pending")
    public String listPendingUsers(Model model, org.springframework.security.core.Authentication authentication) {
        model.addAttribute("users", userService.getPendingUsers());
        
        // Đồng bộ mode dựa trên role của người đang đăng nhập
        String mode = "MANAGEMENT";
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            mode = "ADMIN_MAIN";
        }
        
        model.addAttribute("mode", mode);
        model.addAttribute("isPendingPage", true);
        return "admin/user-list";
    }

    @GetMapping("/approve/{id}")
    public String approveUser(@PathVariable("id") Long id) {
        userService.approveUser(id);
        return "redirect:/admin/users/pending";
    }

    @GetMapping("/reject/{id}")
    public String rejectUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users/pending";
    }
}
