package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DirectMessageRequest {

    @NotNull
    private Long receiverId;

    @NotBlank
    private String content;
}
