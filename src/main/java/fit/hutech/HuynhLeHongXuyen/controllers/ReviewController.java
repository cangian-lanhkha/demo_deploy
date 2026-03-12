package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/books/{bookId}/reviews")
    public String addReview(@PathVariable Long bookId,
                            @RequestParam int rating,
                            @RequestParam(required = false) String comment,
                            @AuthenticationPrincipal User user,
                            RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }
        try {
            reviewService.addReview(bookId, user, rating, comment);
            redirectAttributes.addFlashAttribute("success", "Đánh giá của bạn đã được gửi!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books/" + bookId;
    }

    @PostMapping("/books/{bookId}/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable Long bookId,
                               @PathVariable Long reviewId,
                               @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/login";
        }
        try {
            reviewService.deleteReview(reviewId, user);
            redirectAttributes.addFlashAttribute("success", "Đã xóa đánh giá");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/books/" + bookId;
    }
}
