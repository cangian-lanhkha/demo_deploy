package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.Review;
import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.repositories.IBookRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IItemInvoiceRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewService {
    private final IReviewRepository reviewRepository;
    private final IBookRepository bookRepository;
    private final IItemInvoiceRepository itemInvoiceRepository;

    public Page<Review> getReviewsByBook(Long bookId, Pageable pageable) {
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId, pageable);
    }

    public Review addReview(Long bookId, User user, int rating, String comment) {
        // Check if already reviewed
        if (reviewRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            throw new IllegalStateException("Bạn đã đánh giá sách này rồi");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Sách không tồn tại"));

        Review review = Review.builder()
                .user(user)
                .book(book)
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();
        Review saved = reviewRepository.save(review);

        // Update book average rating
        updateBookRating(bookId);

        log.info("User {} reviewed book {} with {} stars", user.getUsername(), bookId, rating);
        return saved;
    }

    public void deleteReview(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Đánh giá không tồn tại"));

        boolean isOwner = review.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new SecurityException("Bạn không có quyền xóa đánh giá này");
        }

        Long bookId = review.getBook().getId();
        reviewRepository.delete(review);
        updateBookRating(bookId);
    }

    public boolean hasUserReviewed(Long userId, Long bookId) {
        return reviewRepository.existsByUserIdAndBookId(userId, bookId);
    }

    private void updateBookRating(Long bookId) {
        Double avg = reviewRepository.getAverageRatingByBookId(bookId);
        int count = reviewRepository.countByBookId(bookId);
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null) {
            book.setAverageRating(avg != null ? avg : 0.0);
            book.setTotalReviews(count);
            bookRepository.save(book);
        }
    }
}
