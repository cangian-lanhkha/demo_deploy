package fit.hutech.HuynhLeHongXuyen.controllers;

import fit.hutech.HuynhLeHongXuyen.entities.User;
import fit.hutech.HuynhLeHongXuyen.repositories.IUserRepository;
import fit.hutech.HuynhLeHongXuyen.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final IUserRepository userRepository;

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> getNotifications(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.ok(Map.of("notifications", java.util.List.of(), "unreadCount", 0));
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null)
            return ResponseEntity.ok(Map.of("notifications", java.util.List.of(), "unreadCount", 0));
        var notifications = notificationService.getNotificationsForUser(user);
        long unreadCount = notificationService.countUnreadForUser(user);
        var notifList = notifications.stream().limit(10).map(n -> Map.of(
                "id", n.getId(), "title", n.getTitle(), "message", n.getMessage() != null ? n.getMessage() : "",
                "type", n.getType() != null ? n.getType() : "INFO", "couponCode",
                n.getCouponCode() != null ? n.getCouponCode() : "",
                "isRead", n.getIsRead(), "createdAt", n.getCreatedAt().toString())).toList();
        return ResponseEntity.ok(Map.of("notifications", notifList, "unreadCount", unreadCount));
    }

    @PostMapping("/read/{id}")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/read-all")
    @ResponseBody
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.ok(Map.of("success", false));
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user != null)
            notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
