package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServerRequest {

    @NotBlank(message = "Tên server không được để trống")
    private String name;
}
