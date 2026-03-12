package fit.hutech.HuynhLeHongXuyen.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "item_invoices")
public class ItemInvoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    // Cho phép book = null (khi admin xóa sách, đơn hàng vẫn giữ nguyên thông tin)
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = true, foreignKey = @ForeignKey(name = "FK_item_invoice_book", foreignKeyDefinition = "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE SET NULL"))
    private Book book;

    private int quantity;
    private Double price;

    @Column(name = "book_title")
    private String bookTitle;

    @Column(name = "book_author")
    private String bookAuthor;
}
