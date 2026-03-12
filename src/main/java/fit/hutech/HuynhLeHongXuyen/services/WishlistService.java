package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.entities.Wishlist;
import fit.hutech.HuynhLeHongXuyen.repositories.IBookRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IWishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WishlistService {
    private final IWishlistRepository wishlistRepository;
    private final IBookRepository bookRepository;

    public List<Wishlist> getWishlistByUser(Long userId) {
        return wishlistRepository.findByUserIdOrderByAddedAtDesc(userId);
    }

    public boolean toggleWishlist(Long bookId, User user) {
        if (wishlistRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            wishlistRepository.deleteByUserIdAndBookId(user.getId(), bookId);
            log.info("User {} removed book {} from wishlist", user.getUsername(), bookId);
            return false; // removed
        } else {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Sách không tồn tại"));
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .book(book)
                    .addedAt(LocalDateTime.now())
                    .build();
            wishlistRepository.save(wishlist);
            log.info("User {} added book {} to wishlist", user.getUsername(), bookId);
            return true; // added
        }
    }

    public boolean isInWishlist(Long userId, Long bookId) {
        return wishlistRepository.existsByUserIdAndBookId(userId, bookId);
    }

    public int getWishlistCount(Long userId) {
        return wishlistRepository.findByUserIdOrderByAddedAtDesc(userId).size();
    }
}
