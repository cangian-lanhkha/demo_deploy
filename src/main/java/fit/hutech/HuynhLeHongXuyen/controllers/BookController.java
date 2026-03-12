package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.Review;
import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.services.BookService;
import fit.hutech.HuynhLeHongXuyen.services.CategoryService;
import fit.hutech.HuynhLeHongXuyen.services.ReviewService;
import fit.hutech.HuynhLeHongXuyen.services.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final WishlistService wishlistService;

    @GetMapping
    public String listBooks(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "12") int size,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) Double minPrice,
                            @RequestParam(required = false) Double maxPrice,
                            @RequestParam(required = false) Double minRating,
                            @RequestParam(required = false) Boolean inStock,
                            @RequestParam(defaultValue = "newest") String sort,
                            Model model) {
        Sort sortOrder = switch (sort) {
            case "price-asc" -> Sort.by("price").ascending();
            case "price-desc" -> Sort.by("price").descending();
            case "rating" -> Sort.by("averageRating").descending();
            case "bestseller" -> Sort.by("soldCount").descending();
            case "name" -> Sort.by("title").ascending();
            default -> Sort.by("id").descending();
        };
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<Book> bookPage = bookService.advancedSearch(keyword, categoryId, minPrice, maxPrice, minRating, inStock, pageable);

        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("totalItems", bookPage.getTotalElements());
        model.addAttribute("categories", categoryService.getAllCategories());

        // Preserve filter params
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minRating", minRating);
        model.addAttribute("inStock", inStock);
        model.addAttribute("sort", sort);
        return "book/list";
    }

    @GetMapping("/{id}")
    public String bookDetail(@PathVariable Long id,
                             @RequestParam(defaultValue = "0") int reviewPage,
                             @AuthenticationPrincipal User user,
                             Model model) {
        Book book = bookService.getBookById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sách không tồn tại"));

        Page<Review> reviews = reviewService.getReviewsByBook(id, PageRequest.of(reviewPage, 5));

        model.addAttribute("book", book);
        model.addAttribute("reviews", reviews);

        if (book.getCategory() != null) {
            model.addAttribute("relatedBooks", bookService.getRelatedBooks(book.getCategory().getId(), book.getId()));
        }

        if (user != null) {
            model.addAttribute("hasReviewed", reviewService.hasUserReviewed(user.getId(), id));
            model.addAttribute("inWishlist", wishlistService.isInWishlist(user.getId(), id));
        }

        return "book/detail";
    }
}
