package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.services.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping("/wishlist")
    public String viewWishlist(@AuthenticationPrincipal User user, Model model) {
        if (user == null) return "redirect:/login";
        model.addAttribute("wishlistItems", wishlistService.getWishlistByUser(user.getId()));
        return "wishlist/index";
    }

    @PostMapping("/wishlist/toggle/{bookId}")
    public String toggleWishlist(@PathVariable Long bookId,
                                 @AuthenticationPrincipal User user,
                                 @RequestHeader(value = "Referer", required = false) String referer,
                                 RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/login";
        boolean added = wishlistService.toggleWishlist(bookId, user);
        redirectAttributes.addFlashAttribute("success",
                added ? "Đã thêm vào danh sách yêu thích" : "Đã xóa khỏi danh sách yêu thích");
        if (referer != null && !referer.isBlank() && referer.startsWith("/")) {
            return "redirect:" + referer;
        }
        return "redirect:/books/" + bookId;
    }
}
