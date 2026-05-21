package hoangtlt.config;

import hoangtlt.services.UsersService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UsersService usersService;
    private final hoangtlt.services.RestaurantSettingService settingService;

    public GlobalControllerAdvice(UsersService usersService, hoangtlt.services.RestaurantSettingService settingService) {
        this.usersService = usersService;
        this.settingService = settingService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication authentication) {
        // Luôn cung cấp tên quán cho toàn hệ thống
        model.addAttribute("restaurantName", settingService.getSettings().getRestaurantName());

        if (authentication != null && authentication.isAuthenticated()) {
            boolean isManagement = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER"));
            
            if (isManagement) {
                model.addAttribute("pendingUserCount", usersService.getPendingCount());
            }
        }
    }
}
