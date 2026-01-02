package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRolesRequest {
    @NotNull
    private List<String> roles; // ví dụ ["ADMIN","USER_DEFAULT"]
}
