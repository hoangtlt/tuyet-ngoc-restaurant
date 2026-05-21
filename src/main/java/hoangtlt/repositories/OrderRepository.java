package hoangtlt.repositories;

import hoangtlt.entities.SaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

@Repository
public interface OrderRepository extends JpaRepository<SaleOrder, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM SaleOrder o WHERE o.table.id = :tableId AND o.status = :status")
    Optional<SaleOrder> findByTableIdAndStatusWithLock(@Param("tableId") Long tableId,
            @Param("status") SaleOrder.OrderStatus status);

    Optional<SaleOrder> findByTableIdAndStatus(Long tableId, SaleOrder.OrderStatus status);

    Page<SaleOrder> findByCreatedAtAfter(LocalDateTime date, Pageable pageable);

    Page<SaleOrder> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<SaleOrder> findByStaffIdAndCreatedAtAfter(Long staffId, LocalDateTime date, Pageable pageable);

    // Dashboard queries
    List<SaleOrder> findByStatusAndCreatedAtBetween(SaleOrder.OrderStatus status, LocalDateTime start,
            LocalDateTime end);

    List<SaleOrder> findByStatusOrderByCreatedAtDesc(SaleOrder.OrderStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM SaleOrder o WHERE o.status = 'PENDING'")
    long countPendingOrders();

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM SaleOrder o WHERE o.status = :status AND o.createdAt >= :start AND o.createdAt < :end")
    BigDecimal sumTotalAmountByStatusAndCreatedAtBetween(@Param("status") SaleOrder.OrderStatus status,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM SaleOrder o WHERE o.table.area.name = :areaName AND o.status = :status AND o.createdAt BETWEEN :start AND :end")
    long countByAreaNameAndStatusAndCreatedAtBetween(@Param("areaName") String areaName,
            @Param("status") SaleOrder.OrderStatus status, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime date);
}
