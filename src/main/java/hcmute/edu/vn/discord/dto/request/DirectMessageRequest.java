package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DirectMessageRequest {

    @NotNull
    private Long receiverId;

    private String content;

    private List<String> attachments;
    private String replyToId;
}
