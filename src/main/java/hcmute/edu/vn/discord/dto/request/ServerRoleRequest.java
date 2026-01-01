package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class ServerRoleRequest {
    @NotBlank(message = "Tên role không được để trống")
    @Size(max = 100, message = "Tên role không được vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Độ ưu tiên (priority) là bắt buộc")
    private Integer priority;

    // Danh sách code permission muốn gán cho role
    private Set<String> permissionCodes;
}