package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Invoice;
import fit.hutech.HuynhLeHongXuyen.entities.ItemInvoice;
import fit.hutech.HuynhLeHongXuyen.entities.OrderStatusHistory;
import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.entities.enums.OrderStatus;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentStatus;
import fit.hutech.HuynhLeHongXuyen.repositories.IBookRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IInvoiceRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IOrderStatusHistoryRepository;
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
public class OrderService {
    private final IInvoiceRepository invoiceRepository;
    private final IOrderStatusHistoryRepository statusHistoryRepository;
    private final IBookRepository bookRepository;
    private final EmailService emailService;

    public List<Invoice> getAllOrders() {
        return invoiceRepository.findAllByOrderByOrderDateDesc();
    }

    public List<Invoice> getOrdersByStatus(OrderStatus status) {
        return invoiceRepository.findByStatusOrderByOrderDateDesc(status);
    }

    public Invoice getOrderById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại: " + id));
    }

    public List<OrderStatusHistory> getOrderHistory(Long orderId) {
        return statusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId);
    }

    /**
     * Update order status with history tracking and email notification.
     * Also handles stock restoration on cancellation and stock deduction on confirmation.
     */
    public Invoice updateOrderStatus(Long orderId, OrderStatus newStatus, String note, User changedBy) {
        Invoice order = getOrderById(orderId);
        OrderStatus oldStatus = order.getStatus();

        validateStatusTransition(oldStatus, newStatus);

        // Handle inventory changes
        if (newStatus == OrderStatus.CONFIRMED && oldStatus == OrderStatus.PENDING) {
            deductStock(order);
        } else if (newStatus == OrderStatus.CANCELLED &&
                   (oldStatus == OrderStatus.CONFIRMED || oldStatus == OrderStatus.PROCESSING)) {
            restoreStock(order);
        }

        // Update payment status on completion/cancellation
        if (newStatus == OrderStatus.COMPLETED) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (newStatus == OrderStatus.CANCELLED) {
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            } else {
                order.setPaymentStatus(PaymentStatus.FAILED);
            }
        }

        order.setStatus(newStatus);
        Invoice saved = invoiceRepository.save(order);

        // Record status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(saved)
                .status(newStatus)
                .note(note != null ? note : oldStatus.getDisplayName() + " → " + newStatus.getDisplayName())
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
        statusHistoryRepository.save(history);

        log.info("Order #{} status: {} → {} by {}", orderId, oldStatus, newStatus,
                changedBy != null ? changedBy.getUsername() : "system");

        // Send email notification async
        emailService.sendOrderStatusUpdate(saved);

        return saved;
    }

    /**
     * Update tracking info for an order.
     */
    public Invoice updateTracking(Long orderId, String trackingNumber, String shippingProvider) {
        Invoice order = getOrderById(orderId);
        order.setTrackingNumber(trackingNumber);
        order.setShippingProvider(shippingProvider);
        return invoiceRepository.save(order);
    }

    /**
     * User confirms receipt of delivered order.
     */
    public Invoice confirmReceived(Long orderId, User user) {
        Invoice order = getOrderById(orderId);
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền xác nhận đơn hàng này");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Chỉ có thể xác nhận nhận hàng khi đơn ở trạng thái 'Đã giao hàng'");
        }
        return updateOrderStatus(orderId, OrderStatus.COMPLETED, "Khách hàng xác nhận đã nhận hàng", user);
    }

    /**
     * User cancels a pending order.
     */
    public Invoice cancelOrder(Long orderId, User user, String reason) {
        Invoice order = getOrderById(orderId);
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền hủy đơn hàng này");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng khi ở trạng thái 'Chờ xác nhận'");
        }
        return updateOrderStatus(orderId, OrderStatus.CANCELLED,
                "Khách hàng hủy đơn" + (reason != null ? ": " + reason : ""), user);
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED;
            case CONFIRMED -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case PROCESSING -> to == OrderStatus.SHIPPING || to == OrderStatus.CANCELLED;
            case SHIPPING -> to == OrderStatus.DELIVERED;
            case DELIVERED -> to == OrderStatus.COMPLETED || to == OrderStatus.REFUNDED;
            case COMPLETED, CANCELLED, REFUNDED -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Không thể chuyển trạng thái từ '" + from.getDisplayName() + "' sang '" + to.getDisplayName() + "'");
        }
    }

    private void deductStock(Invoice order) {
        if (order.getItems() == null) return;
        for (ItemInvoice item : order.getItems()) {
            if (item.getBook() != null) {
                int updated = bookRepository.decreaseStock(item.getBook().getId(), item.getQuantity());
                if (updated == 0) {
                    throw new IllegalStateException(
                            "Sách '" + item.getBookTitle() + "' không đủ số lượng trong kho");
                }
            }
        }
    }

    private void restoreStock(Invoice order) {
        if (order.getItems() == null) return;
        for (ItemInvoice item : order.getItems()) {
            if (item.getBook() != null) {
                bookRepository.increaseStock(item.getBook().getId(), item.getQuantity());
            }
        }
    }
}
