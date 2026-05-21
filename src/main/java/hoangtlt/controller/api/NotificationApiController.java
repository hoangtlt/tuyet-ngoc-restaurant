package hoangtlt.controller.api;

import hoangtlt.services.UsersService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final UsersService usersService;

    public NotificationApiController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping("/pending-users-count")
    public Map<String, Long> getPendingUsersCount() {
        return Map.of("count", usersService.getPendingCount());
    }
}
