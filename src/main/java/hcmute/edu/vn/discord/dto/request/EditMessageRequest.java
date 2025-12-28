package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotBlank;

public class EditMessageRequest {

    @NotBlank
    private String content;

    // Getters and setters
}
