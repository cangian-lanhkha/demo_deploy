package fit.hutech.HuynhLeHongXuyen.repositories;

import fit.hutech.HuynhLeHongXuyen.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double getAverageRatingByBookId(Long bookId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.book.id = :bookId")
    int countByBookId(Long bookId);
}
