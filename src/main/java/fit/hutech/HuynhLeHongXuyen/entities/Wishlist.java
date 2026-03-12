package fit.hutech.HuynhLeHongXuyen.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "wishlists", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "book_id" })
})
public class Wishlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @ToString.Exclude
    private Book book;

    @Builder.Default
    private LocalDateTime addedAt = LocalDateTime.now();
}
