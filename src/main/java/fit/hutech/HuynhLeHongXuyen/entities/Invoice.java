package fit.hutech.HuynhLeHongXuyen.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fit.hutech.HuynhLeHongXuyen.entities.enums.OrderStatus;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentMethod;
import fit.hutech.HuynhLeHongXuyen.entities.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_order_date", columnList = "orderDate"),
    @Index(name = "idx_invoice_status", columnList = "status"),
    @Index(name = "idx_invoice_user", columnList = "user_id"),
    @Index(name = "idx_invoice_order_code", columnList = "orderCode"),
    @Index(name = "idx_invoice_payment_status", columnList = "paymentStatus")
})
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String orderCode;

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    private String phone;
    private String email;
    private String address;

    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    private Double totalPrice;

    @Builder.Default
    private Double shippingFee = 0.0;

    @Builder.Default
    private Double discount = 0.0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String trackingNumber;
    private String shippingProvider;

    @Column(length = 500)
    private String notes;

    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private List<ItemInvoice> items;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    private List<OrderStatusHistory> statusHistory;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
