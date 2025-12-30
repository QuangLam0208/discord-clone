package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    // Tạo Category cần serverId, cập nhật chỉ cần name
    @NotNull(message = "serverId là bắt buộc khi tạo category")
    private Long serverId;

    @NotBlank(message = "Tên category không được để trống")
    @Size(min = 1, max = 100, message = "Tên category phải từ 1 đến 100 ký tự")
    private String name;

    public void normalize() {
        if (name != null) {
            name = name.trim();
            if (name.isBlank()) {
                name = null; // để trigger @NotBlank
            }
        }
    }
}