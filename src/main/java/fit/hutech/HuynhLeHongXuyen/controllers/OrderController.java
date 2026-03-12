package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.Invoice;
import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.repositories.IInvoiceRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IUserRepository;
import fit.hutech.HuynhLeHongXuyen.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final IInvoiceRepository invoiceRepository;
    private final IUserRepository userRepository;
    private final OrderService orderService;

    @GetMapping
    public String listOrders(Authentication authentication, Model model) {
        List<Invoice> orders;
        if (authentication != null) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            orders = (user != null) ? invoiceRepository.findByUserOrderByOrderDateDesc(user) : Collections.emptyList();
        } else {
            orders = Collections.emptyList();
        }
        model.addAttribute("orders", orders);
        return "order/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Authentication authentication, Model model) {
        Invoice order = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại: " + id));
        if (authentication != null) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin && (order.getUser() == null || !order.getUser().equals(user)))
                return "redirect:/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("history", orderService.getOrderHistory(id));
        return "order/detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            orderService.cancelOrder(id, user, reason);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng #" + id + " thành công");
        } catch (IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/confirm-received")
    public String confirmReceived(@PathVariable Long id,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            orderService.confirmReceived(id, user);
            redirectAttributes.addFlashAttribute("success", "Đã xác nhận nhận hàng thành công!");
        } catch (IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
