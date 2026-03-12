package fit.hutech.HuynhLeHongXuyen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String message;

    private String couponCode;

    @Column(nullable = false)
    private String type;

    @Builder.Default
    private Boolean isRead = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
