package fit.hutech.HuynhLeHongXuyen.repositories;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String keyword);

    Page<Book> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE " +
            "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR b.category.id = :categoryId) " +
            "AND (:minPrice IS NULL OR b.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR b.price <= :maxPrice) " +
            "AND (:minRating IS NULL OR b.averageRating >= :minRating) " +
            "AND (:inStock IS NULL OR (:inStock = true AND b.quantity > 0) OR :inStock = false)")
    Page<Book> advancedSearch(@Param("keyword") String keyword,
                              @Param("categoryId") Long categoryId,
                              @Param("minPrice") Double minPrice,
                              @Param("maxPrice") Double maxPrice,
                              @Param("minRating") Double minRating,
                              @Param("inStock") Boolean inStock,
                              Pageable pageable);

    List<Book> findByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    List<Book> findTop8ByCategoryIdAndIdNot(Long categoryId, Long bookId);

    @Modifying
    @Query("UPDATE Book b SET b.quantity = b.quantity - :qty, b.soldCount = b.soldCount + :qty WHERE b.id = :bookId AND b.quantity >= :qty")
    int decreaseStock(@Param("bookId") Long bookId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Book b SET b.quantity = b.quantity + :qty WHERE b.id = :bookId")
    int increaseStock(@Param("bookId") Long bookId, @Param("qty") int qty);
}
