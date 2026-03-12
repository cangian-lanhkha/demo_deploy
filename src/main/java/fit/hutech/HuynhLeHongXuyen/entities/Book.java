package fit.hutech.HuynhLeHongXuyen.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "books", indexes = {
    @Index(name = "idx_book_title", columnList = "title"),
    @Index(name = "idx_book_category", columnList = "category_id"),
    @Index(name = "idx_book_price", columnList = "price"),
    @Index(name = "idx_book_rating", columnList = "averageRating"),
    @Index(name = "idx_book_sold", columnList = "soldCount")
})
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tiêu đề sách không được để trống")
    @Size(max = 255, message = "Tiêu đề sách không được vượt quá 255 ký tự")
    private String title;

    @Size(max = 255, message = "Tên tác giả không được vượt quá 255 ký tự")
    private String author;

    @Min(value = 0, message = "Giá sách phải lớn hơn hoặc bằng 0")
    private Double price;

    private String image;

    @Size(max = 20, message = "ISBN không được vượt quá 20 ký tự")
    private String isbn;

    @Size(max = 255, message = "NXB không được vượt quá 255 ký tự")
    private String publisher;

    private Integer publishYear;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity = 0;

    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer totalReviews = 0;

    @Builder.Default
    private Integer soldCount = 0;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
