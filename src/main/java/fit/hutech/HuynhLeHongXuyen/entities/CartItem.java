package fit.hutech.HuynhLeHongXuyen.entities;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    private Book book;
    private int quantity;

    public Double getSubtotal() {
        return book.getPrice() * quantity;
    }
}
