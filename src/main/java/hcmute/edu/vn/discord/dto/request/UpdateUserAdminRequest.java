package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserAdminRequest {
    @Size(max = 100)
    private String displayName;
    private List<String> roles; // Ví dụ: ["ADMIN","USER_DEFAULT","USER_PREMIUM"]
}
