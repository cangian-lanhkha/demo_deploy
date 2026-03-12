package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.*;
import fit.hutech.HuynhLeHongXuyen.entities.enums.OrderStatus;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentMethod;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentStatus;
import fit.hutech.HuynhLeHongXuyen.repositories.IInvoiceRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IUserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InvoiceService {
    private final IInvoiceRepository invoiceRepository;
    private final IUserRepository userRepository;
    private final CartService cartService;

    /**
     * Create invoice with idempotency check (orderCode unique).
     * Returns existing invoice if orderCode already processed.
     */
    public Invoice createInvoice(String customerName, String phone, String email, String address,
            String orderCode, PaymentMethod paymentMethod, HttpSession session) {
        // Idempotency: check if invoice already exists for this orderCode
        if (orderCode != null && invoiceRepository.existsByOrderCode(orderCode)) {
            log.warn("Invoice already exists for orderCode: {}", orderCode);
            return invoiceRepository.findByOrderCode(orderCode).orElse(null);
        }

        List<CartItem> cartItems = cartService.getCart(session);
        if (cartItems.isEmpty())
            throw new IllegalStateException("Giỏ hàng trống, không thể tạo hóa đơn");

        User currentUser = getCurrentUser();

        Double discount = session.getAttribute("payment_discount") != null
                ? (Double) session.getAttribute("payment_discount") : 0.0;

        Invoice invoice = Invoice.builder()
                .orderCode(orderCode)
                .customerName(customerName)
                .phone(phone)
                .email(email)
                .address(address)
                .orderDate(LocalDateTime.now())
                .totalPrice(cartService.getTotal(session))
                .discount(discount)
                .status(OrderStatus.PENDING)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentMethod == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.PAID)
                .user(currentUser)
                .build();

        List<ItemInvoice> items = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            ItemInvoice itemInvoice = ItemInvoice.builder().invoice(invoice).book(cartItem.getBook())
                    .quantity(cartItem.getQuantity()).price(cartItem.getBook().getPrice())
                    .bookTitle(cartItem.getBook().getTitle()).bookAuthor(cartItem.getBook().getAuthor()).build();
            items.add(itemInvoice);
        }
        invoice.setItems(items);
        Invoice savedInvoice = invoiceRepository.save(invoice);
        cartService.clearCart(session);
        return savedInvoice;
    }

    /**
     * Backward-compatible overload for existing callers.
     */
    public Invoice createInvoice(String customerName, String phone, String email, String address,
            HttpSession session) {
        String orderCode = (String) session.getAttribute("payment_orderCode");
        return createInvoice(customerName, phone, email, address, orderCode, PaymentMethod.QR_BANK, session);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }
        return null;
    }
}
