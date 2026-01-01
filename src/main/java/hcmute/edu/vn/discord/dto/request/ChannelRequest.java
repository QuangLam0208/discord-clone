package hcmute.edu.vn.discord.dto.request;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChannelRequest {
    @NotBlank(message = "Tên kênh không được để trống")
    @Size(max = 100, message = "Tên kênh phải từ 1 đến 100 ký tự")
    private String name;

    @NotNull(message = "Loại kênh là bắt buộc (TEXT hoặc VOICE)")
    private ChannelType type;

    private Boolean isPrivate;
    private Long categoryId;
    public void normalize() {
        if (name != null) {
            name = name.trim();
            if (type == ChannelType.TEXT) {
                name = name.toLowerCase().replace(' ', '-');
            }
        }
    }
}