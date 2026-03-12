package fit.hutech.HuynhLeHongXuyen.viewmodels;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookGetVm {
    private Long id;
    private String title;
    private String author;
    private Double price;
    private String image;
    private String categoryName;
}
