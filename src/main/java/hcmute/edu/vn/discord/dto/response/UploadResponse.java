package hcmute.edu.vn.discord.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for file upload operations.
 *
 * Contains the URL of the uploaded file and metadata about the upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    /**
     * Public URL to access the uploaded file.
     * Format: http://localhost:8081/files/yyyy/MM/dd/uuid.ext
     */
    private String url;

    /**
     * Original filename provided by the client (sanitized).
     */
    private String originalFilename;

    /**
     * Actual filename stored on the server (UUID-based).
     */
    private String filename;

    /**
     * Content type of the uploaded file (e.g., "image/jpeg").
     */
    private String contentType;

    /**
     * Size of the uploaded file in bytes.
     */
    private Long size;

    /**
     * Timestamp when the file was uploaded.
     */
    private LocalDateTime uploadedAt;

    /**
     * Username of the user who uploaded the file.
     */
    private String uploadedBy;
}
