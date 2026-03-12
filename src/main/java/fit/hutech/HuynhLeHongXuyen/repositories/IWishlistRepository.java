package fit.hutech.HuynhLeHongXuyen.repositories;

import fit.hutech.HuynhLeHongXuyen.entities.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IWishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<Wishlist> findByUserIdAndBookId(Long userId, Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    void deleteByUserIdAndBookId(Long userId, Long bookId);
}
