package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditMessageRequest {

    @NotBlank(message = "Content cannot be blank")
    @Size(max = 2000, message = "Content too long")
    private String content;
}
