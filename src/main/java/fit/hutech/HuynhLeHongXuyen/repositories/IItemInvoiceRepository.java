package fit.hutech.HuynhLeHongXuyen.repositories;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.ItemInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IItemInvoiceRepository extends JpaRepository<ItemInvoice, Long> {
    List<ItemInvoice> findByBook(Book book);

    @Query("SELECT COALESCE(ii.bookTitle, ii.book.title), SUM(ii.quantity), SUM(ii.price * ii.quantity) " +
            "FROM ItemInvoice ii WHERE ii.invoice.orderDate BETWEEN :from AND :to " +
            "AND ii.invoice.status NOT IN ('CANCELLED','REFUNDED') " +
            "GROUP BY COALESCE(ii.bookTitle, ii.book.title) ORDER BY SUM(ii.quantity) DESC")
    List<Object[]> topSellingBooks(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT ii.invoice.user.username, COUNT(DISTINCT ii.invoice.id), SUM(ii.invoice.totalPrice) " +
            "FROM ItemInvoice ii WHERE ii.invoice.orderDate BETWEEN :from AND :to " +
            "AND ii.invoice.status NOT IN ('CANCELLED','REFUNDED') AND ii.invoice.user IS NOT NULL " +
            "GROUP BY ii.invoice.user.username ORDER BY SUM(ii.invoice.totalPrice) DESC")
    List<Object[]> topCustomers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
