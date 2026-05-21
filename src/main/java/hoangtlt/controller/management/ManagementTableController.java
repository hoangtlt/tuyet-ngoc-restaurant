package hoangtlt.controller.management;

import hoangtlt.entities.DiningTable;
import hoangtlt.services.AreaService;
import hoangtlt.services.DiningTableService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/management/tables")
public class ManagementTableController {

    private final DiningTableService tableService;
    private final AreaService areaService;

    public ManagementTableController(DiningTableService tableService, AreaService areaService) {
        this.tableService = tableService;
        this.areaService = areaService;
    }

    @GetMapping
    public String listTables(@RequestParam(value = "areaId", required = false) Long areaId, Model model) {
        java.util.List<hoangtlt.entities.Area> areas = areaService.getAllAreas();
        
        // Nếu không truyền areaId, mặc định chọn khu vực đầu tiên không phải "MANG VỀ"
        if (areaId == null) {
            areaId = areas.stream()
                .filter(a -> !a.getName().equalsIgnoreCase("MANG VỀ"))
                .map(hoangtlt.entities.Area::getId)
                .findFirst()
                .orElse(null);
        }

        java.util.List<DiningTable> tables;
        if (areaId != null && areaId > 0) {
            tables = tableService.getTablesByArea(areaId);
            model.addAttribute("selectedAreaId", areaId);
        } else {
            tables = tableService.getAllTables();
        }
        
        // Sắp xếp bàn theo số thứ tự tự nhiên (tương tự như trang chủ)
        tables.sort((t1, t2) -> {
            String s1 = t1.getTableNumber();
            String s2 = t2.getTableNumber();
            try {
                Double n1 = Double.parseDouble(s1.replaceAll("[^0-9.]", ""));
                Double n2 = Double.parseDouble(s2.replaceAll("[^0-9.]", ""));
                return n1.compareTo(n2);
            } catch (Exception e) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        
        model.addAttribute("tables", tables);
        model.addAttribute("areas", areas);
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "tables");
        return "management/table-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("diningTable", new DiningTable());
        model.addAttribute("areas", areaService.getAllAreas());
        model.addAttribute("title", "Thêm bàn mới");
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "tables");
        return "management/table-form";
    }

    @PostMapping("/save")
    public String saveTable(@ModelAttribute("diningTable") DiningTable table) {
        tableService.saveTable(table);
        return "redirect:/management/tables";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        DiningTable table = tableService.getTableById(id).orElseThrow();
        model.addAttribute("diningTable", table);
        model.addAttribute("areas", areaService.getAllAreas());
        model.addAttribute("title", "Chỉnh sửa bàn");
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "tables");
        return "management/table-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteTable(@PathVariable("id") Long id) {
        tableService.deleteTable(id);
        return "redirect:/management/tables";
    }
}
