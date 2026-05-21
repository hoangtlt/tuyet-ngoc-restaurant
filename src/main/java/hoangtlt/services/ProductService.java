package hoangtlt.services;

import hoangtlt.entities.Product;
import hoangtlt.repositories.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public void adjustPrices(BigDecimal amount, Double percentage, Long categoryId) {
        if (categoryId != null && categoryId > 0) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
                productRepository.adjustPricesByCategoryAndAmount(categoryId, amount);
            } else if (percentage != null && percentage != 0) {
                productRepository.adjustPricesByCategoryAndPercentage(categoryId, percentage);
            }
        } else {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
                productRepository.adjustAllPricesByAmount(amount);
            } else if (percentage != null && percentage != 0) {
                productRepository.adjustAllPricesByPercentage(percentage);
            }
        }
    }
}
