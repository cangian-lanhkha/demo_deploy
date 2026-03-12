package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Book;
import fit.hutech.HuynhLeHongXuyen.entities.CartItem;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {
    private static final String CART_SESSION_KEY = "cart";

    @SuppressWarnings("unchecked")
    public List<CartItem> getCart(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    public void addToCart(HttpSession session, Book book) {
        List<CartItem> cart = getCart(session);
        Optional<CartItem> existingItem = cart.stream().filter(item -> item.getBook().getId().equals(book.getId()))
                .findFirst();
        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + 1);
        } else {
            cart.add(new CartItem(book, 1));
        }
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    public void removeFromCart(HttpSession session, Long bookId) {
        List<CartItem> cart = getCart(session);
        cart.removeIf(item -> item.getBook().getId().equals(bookId));
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    public void updateQuantity(HttpSession session, Long bookId, int quantity) {
        List<CartItem> cart = getCart(session);
        cart.stream().filter(item -> item.getBook().getId().equals(bookId)).findFirst().ifPresent(item -> {
            if (quantity <= 0) {
                cart.removeIf(i -> i.getBook().getId().equals(bookId));
            } else {
                item.setQuantity(quantity);
            }
        });
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
    }

    public double getTotal(HttpSession session) {
        return getCart(session).stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public int getCount(HttpSession session) {
        return getCart(session).stream().mapToInt(CartItem::getQuantity).sum();
    }
}
