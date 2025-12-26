package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerRequest {

    @NotBlank(message = "Tên server không được để trống")
    @Size(max = 100, message = "Tên server không được dài quá 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String description;

    @Size(max = 255, message = "Icon URL không hợp lệ")
    private String iconUrl;

    public void normalize() {
        if (name != null) name = name.trim();
        if (description != null) description = description.trim();
        if (iconUrl != null) iconUrl = iconUrl.trim();
    }
}

