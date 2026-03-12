package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.CartItem;
import fit.hutech.HuynhLeHongXuyen.services.BookService;
import fit.hutech.HuynhLeHongXuyen.services.CartService;
import fit.hutech.HuynhLeHongXuyen.services.CouponService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final BookService bookService;
    private final CouponService couponService;

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        model.addAttribute("cart", cartService.getCart(session));
        model.addAttribute("total", cartService.getTotal(session));
        model.addAttribute("count", cartService.getCount(session));
        return "cart/index";
    }

    @PostMapping("/add/{bookId}")
    public String addToCart(@PathVariable Long bookId, HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("S\u00e1ch kh\u00f4ng t\u1ed3n t\u1ea1i"));
        // Check stock availability
        int currentInCart = cartService.getCart(session).stream()
                .filter(item -> item.getBook().getId().equals(bookId))
                .mapToInt(fit.hutech.HuynhLeHongXuyen.entities.CartItem::getQuantity).sum();
        if (book.getQuantity() != null && book.getQuantity() <= currentInCart) {
            redirectAttributes.addFlashAttribute("error", "S\u00e1ch '" + book.getTitle() + "' \u0111\u00e3 h\u1ebft h\u00e0ng ho\u1eb7c kh\u00f4ng \u0111\u1ee7 s\u1ed1 l\u01b0\u1ee3ng");
            return "redirect:/books";
        }
        cartService.addToCart(session, book);
        return "redirect:/books";
    }

    @PostMapping("/update/{bookId}")
    public String updateQuantity(@PathVariable Long bookId, @RequestParam int quantity, HttpSession session) {
        cartService.updateQuantity(session, bookId, quantity);
        return "redirect:/cart";
    }

    @GetMapping("/remove/{bookId}")
    public String removeFromCart(@PathVariable Long bookId, HttpSession session) {
        cartService.removeFromCart(session, bookId);
        return "redirect:/cart";
    }

    @GetMapping("/clear")
    public String clearCart(HttpSession session) {
        cartService.clearCart(session);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String showCheckout(HttpSession session, Model model) {
        List<CartItem> cart = cartService.getCart(session);
        if (cart.isEmpty())
            return "redirect:/cart";
        model.addAttribute("cart", cart);
        model.addAttribute("total", cartService.getTotal(session));
        model.addAttribute("activeCoupons", couponService.getActiveCoupons());
        return "cart/checkout";
    }
}
