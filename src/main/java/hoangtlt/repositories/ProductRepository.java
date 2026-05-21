package hoangtlt.repositories;

import hoangtlt.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByNameContainingIgnoreCase(String name);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.price = p.price + :amount")
    int adjustAllPricesByAmount(@Param("amount") BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.price = p.price * (1.0 + :percentage / 100.0)")
    int adjustAllPricesByPercentage(@Param("percentage") double percentage);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.price = p.price + :amount WHERE p.category.id = :categoryId")
    int adjustPricesByCategoryAndAmount(@Param("categoryId") Long categoryId, @Param("amount") BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.price = p.price * (1.0 + :percentage / 100.0) WHERE p.category.id = :categoryId")
    int adjustPricesByCategoryAndPercentage(@Param("categoryId") Long categoryId, @Param("percentage") double percentage);
}
