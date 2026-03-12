package fit.hutech.HuynhLeHongXuyen.entities;

import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentMethod;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentStatus;
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
@Table(name = "payment_transactions")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Invoice order;

    @Column(unique = true)
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private Double amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String gatewayResponse;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
