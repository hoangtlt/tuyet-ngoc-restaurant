package hoangtlt.controller.management;

import hoangtlt.entities.Area;
import hoangtlt.services.AreaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/management/areas")
public class ManagementAreaController {

    private final AreaService areaService;

    public ManagementAreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @GetMapping
    public String listAreas(Model model) {
        model.addAttribute("areas", areaService.getAllAreas());
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "areas");
        return "management/area-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("area", new Area());
        model.addAttribute("title", "Thêm khu vực mới");
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "areas");
        return "management/area-form";
    }

    @PostMapping("/save")
    public String saveArea(@ModelAttribute("area") Area area) {
        areaService.saveArea(area);
        return "redirect:/management/areas";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Area area = areaService.getAreaById(id).orElseThrow();
        model.addAttribute("area", area);
        model.addAttribute("title", "Chỉnh sửa khu vực");
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "areas");
        return "management/area-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteArea(@PathVariable("id") Long id) {
        areaService.deleteArea(id);
        return "redirect:/management/areas";
    }
}
