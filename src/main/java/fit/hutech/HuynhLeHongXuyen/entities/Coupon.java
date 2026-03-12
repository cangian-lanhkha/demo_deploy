package fit.hutech.HuynhLeHongXuyen.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    private String description;

    @Column(nullable = false)
    private Double discountPercent;

    private Double maxDiscount;

    private Double minOrderAmount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer usageLimit;

    private Integer usedCount = 0;

    private Boolean active = true;

    public boolean isValid() {
        if (!active)
            return false;
        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate))
            return false;
        if (endDate != null && now.isAfter(endDate))
            return false;
        if (usageLimit != null && usedCount >= usageLimit)
            return false;
        return true;
    }

    public double calculateDiscount(double orderAmount) {
        if (!isValid())
            return 0;
        if (minOrderAmount != null && orderAmount < minOrderAmount)
            return 0;
        double discount = orderAmount * discountPercent / 100.0;
        if (maxDiscount != null && discount > maxDiscount) {
            discount = maxDiscount;
        }
        return Math.round(discount);
    }
}
