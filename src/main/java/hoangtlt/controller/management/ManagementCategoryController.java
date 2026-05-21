package hoangtlt.controller.management;

import hoangtlt.entities.Category;
import hoangtlt.services.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/management/categories")
public class ManagementCategoryController {

    private final CategoryService categoryService;

    public ManagementCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "categories");
        return "management/category-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("title", "Thêm danh mục");
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "categories");
        return "management/category-form";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("category") Category category) {
        categoryService.saveCategory(category);
        return "redirect:/management/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Category category = categoryService.getCategoryById(id).orElseThrow();
        model.addAttribute("category", category);
        model.addAttribute("title", "Chỉnh sửa danh mục");
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "categories");
        return "management/category-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        Category category = categoryService.getCategoryById(id).orElseThrow();
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            ra.addFlashAttribute("error", "Không thể xóa danh mục \"" + category.getName() + "\" vì vẫn còn sản phẩm bên trong!");
            return "redirect:/management/categories";
        }
        categoryService.deleteCategory(id);
        ra.addFlashAttribute("success", "Đã xóa danh mục thành công!");
        return "redirect:/management/categories";
    }
}
