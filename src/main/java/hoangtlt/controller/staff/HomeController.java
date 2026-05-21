package hoangtlt.controller.staff;

import hoangtlt.services.AreaService;
import hoangtlt.services.DiningTableService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Comparator;

@Controller
public class HomeController {

    private final AreaService areaService;
    private final DiningTableService tableService;

    public HomeController(AreaService areaService, DiningTableService tableService) {
        this.areaService = areaService;
        this.tableService = tableService;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        List<hoangtlt.entities.Area> areas = areaService.getAllAreas();
        List<hoangtlt.entities.DiningTable> tables = tableService.getAllTables();

        // Sắp xếp bàn theo số thứ tự tự nhiên (hỗ trợ cả chữ và số)
        tables.sort((t1, t2) -> {
            String s1 = t1.getTableNumber();
            String s2 = t2.getTableNumber();

            try {
                // Trích xuất số từ chuỗi (ví dụ "Bàn 1" -> 1)
                Double n1 = Double.parseDouble(s1.replaceAll("[^0-9.]", ""));
                Double n2 = Double.parseDouble(s2.replaceAll("[^0-9.]", ""));
                return n1.compareTo(n2);
            } catch (Exception e) {
                // Nếu không trích xuất được số, so sánh theo chữ cái
                return s1.compareToIgnoreCase(s2);
            }
        });

        model.addAttribute("areas", areas);
        model.addAttribute("tables", tables);

        // Xác định sidebar mode dựa trên role
        if (authentication != null) {
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                model.addAttribute("mode", "ADMIN_MAIN");
            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER"))) {
                model.addAttribute("mode", "MANAGEMENT");
            } else {
                model.addAttribute("mode", "STAFF");
            }
        }

        return "staff/home";
    }
}
