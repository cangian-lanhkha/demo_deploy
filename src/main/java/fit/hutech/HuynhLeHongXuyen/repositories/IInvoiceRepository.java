package fit.hutech.HuynhLeHongXuyen.repositories;

import fit.hutech.HuynhLeHongXuyen.entities.Invoice;
import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.entities.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IInvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUserOrderByOrderDateDesc(User user);

    Optional<Invoice> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    List<Invoice> findByStatusOrderByOrderDateDesc(OrderStatus status);

    List<Invoice> findAllByOrderByOrderDateDesc();

    // === Report queries ===

    @Query("SELECT CAST(i.orderDate AS date) as day, SUM(i.totalPrice) " +
            "FROM Invoice i WHERE i.orderDate BETWEEN :from AND :to AND i.status NOT IN ('CANCELLED','REFUNDED') " +
            "GROUP BY CAST(i.orderDate AS date) ORDER BY day")
    List<Object[]> revenueByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT i.paymentMethod, SUM(i.totalPrice), COUNT(i) " +
            "FROM Invoice i WHERE i.orderDate BETWEEN :from AND :to AND i.status NOT IN ('CANCELLED','REFUNDED') " +
            "GROUP BY i.paymentMethod")
    List<Object[]> revenueByPaymentMethod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT i.status, COUNT(i) FROM Invoice i WHERE i.orderDate BETWEEN :from AND :to GROUP BY i.status")
    List<Object[]> countByStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT SUM(i.totalPrice) FROM Invoice i WHERE i.orderDate BETWEEN :from AND :to AND i.status NOT IN ('CANCELLED','REFUNDED')")
    Double totalRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.orderDate BETWEEN :from AND :to")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.orderDate BETWEEN :from AND :to AND i.status NOT IN ('CANCELLED','REFUNDED')")
    long countSuccessfulBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<Invoice> findByOrderDateBetweenOrderByOrderDateDesc(LocalDateTime from, LocalDateTime to);
}
