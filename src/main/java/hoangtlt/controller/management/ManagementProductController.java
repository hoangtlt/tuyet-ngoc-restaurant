package hoangtlt.controller.management;

import hoangtlt.entities.Product;
import hoangtlt.services.CategoryService;
import hoangtlt.services.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/management/products")
public class ManagementProductController {

    private static final Logger log = LoggerFactory.getLogger(ManagementProductController.class);
    private final ProductService productService;
    private final CategoryService categoryService;

    public ManagementProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listProducts(Model model) {
        List<Product> products = productService.getAllProducts();
        // Sorting is now handled by productService.getAllProducts()
        
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "products");
        return "management/product-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Thêm món ăn mới");
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "products");
        return "management/product-form";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("product") Product product,
                               @RequestParam("imageFile") MultipartFile imageFile,
                               RedirectAttributes redirectAttributes) throws IOException {
        
        if (!imageFile.isEmpty()) {
            // Validation (Fix bug 4.4, 5.5)
            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chỉ được upload file ảnh!");
                return "redirect:/management/products/add";
            }
            if (imageFile.getSize() > 5 * 1024 * 1024) { // 5MB
                redirectAttributes.addFlashAttribute("errorMessage", "Dung lượng ảnh tối đa 5MB!");
                return "redirect:/management/products/add";
            }

            String originalName = imageFile.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;
            Path uploadPath = Paths.get("uploads");
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(imageFile.getInputStream(), filePath);
            product.setImageUrl("/uploads/" + fileName);
        }
        
        if (product.getVariants() != null) {
            // Loại bỏ các phân loại có giá trống trước để tránh lỗi NullPointerException (Dòng 95)
            product.getVariants().removeIf(v -> v.getPrice() == null);
            
            product.getVariants().forEach(v -> {
                v.setProduct(product);
                if (v.getName() == null || v.getName().isEmpty()) {
                    v.setName(v.getPrice().toString() + "đ");
                }
            });
        }
        
        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu món ăn thành công!");
        return "redirect:/management/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id).orElseThrow();
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Chỉnh sửa món ăn");
        model.addAttribute("mode", "MANAGEMENT");
        model.addAttribute("activePage", "products");
        return "management/product-form";
    }

    @PostMapping("/delete/{id}") // Change from GET to POST (Fix bug 5.4)
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa món ăn thành công!");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa món này vì đã có hóa đơn liên quan. Vui lòng chọn 'Tạm hết món' thay vì xóa.");
        } catch (Exception e) {
            log.error("Lỗi khi xóa sản phẩm {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/management/products";
    }

    @PostMapping("/adjust-prices")
    public String adjustPrices(@RequestParam(value = "amount", required = false) BigDecimal amount,
                               @RequestParam(value = "percentage", required = false) Double percentage,
                               @RequestParam(value = "categoryId", required = false) Long categoryId) {
        productService.adjustPrices(amount, percentage, categoryId);
        return "redirect:/management/products";
    }

    @PostMapping("/toggle-availability/{id}") // Change from GET to POST (Fix bug 5.4)
    public String toggleAvailability(@PathVariable("id") Long id) {
        productService.getProductById(id).ifPresent(product -> {
            product.setAvailable(!product.isAvailable());
            productService.saveProduct(product);
        });
        return "redirect:/management/products";
    }
}
