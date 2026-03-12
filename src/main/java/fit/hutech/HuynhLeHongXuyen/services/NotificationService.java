package fit.hutech.HuynhLeHongXuyen.services;

import fit.hutech.HuynhLeHongXuyen.entities.Notification;
import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.repositories.INotificationRepository;
import fit.hutech.HuynhLeHongXuyen.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final INotificationRepository notificationRepository;
    private final IUserRepository userRepository;

    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrUserIsNullOrderByCreatedAtDesc(user);
    }

    public long countUnreadForUser(User user) {
        long userUnread = notificationRepository.countByUserAndIsReadFalse(user);
        long globalUnread = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(null).size();
        return userUnread + globalUnread;
    }

    public void sendToUser(User user, String title, String message, String type, String couponCode) {
        Notification notification = Notification.builder().user(user).title(title).message(message).type(type)
                .couponCode(couponCode).isRead(false).createdAt(LocalDateTime.now()).build();
        notificationRepository.save(notification);
    }

    public void sendToAll(String title, String message, String type, String couponCode) {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers)
            sendToUser(user, title, message, type, couponCode);
    }

    public void sendCouponToUser(User user, String couponCode, String couponDesc) {
        sendToUser(user, "Bạn nhận được mã giảm giá!",
                "Mã: " + couponCode + " — " + couponDesc + ". Nhập mã khi thanh toán để nhận ưu đãi!", "COUPON",
                couponCode);
    }

    public void sendCouponToAll(String couponCode, String couponDesc) {
        sendToAll("Khuyến mãi mới dành cho bạn!",
                "Mã: " + couponCode + " — " + couponDesc + ". Nhập mã khi thanh toán để nhận ưu đãi!", "COUPON",
                couponCode);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        for (Notification n : unread)
            n.setIsRead(true);
        notificationRepository.saveAll(unread);
    }
}
