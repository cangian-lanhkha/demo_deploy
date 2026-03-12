package fit.hutech.HuynhLeHongXuyen.viewmodels;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookPostVm {
    @NotBlank(message = "Tiêu đề sách không được để trống")
    @Size(max = 255, message = "Tiêu đề sách không được vượt quá 255 ký tự")
    private String title;

    @Size(max = 255, message = "Tên tác giả không được vượt quá 255 ký tự")
    private String author;

    @Min(value = 0, message = "Giá sách phải lớn hơn hoặc bằng 0")
    private Double price;

    private Long categoryId;
}
