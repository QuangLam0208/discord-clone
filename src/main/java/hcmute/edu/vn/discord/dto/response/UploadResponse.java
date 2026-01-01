package hcmute.edu.vn.discord.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String url;
    private String originalFilename;
    private String filename;
    private String contentType;
    private Long size;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
}
