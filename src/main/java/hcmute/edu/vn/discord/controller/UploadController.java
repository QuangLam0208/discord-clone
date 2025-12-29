package hcmute.edu.vn.discord.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    @Value("${server.port}")
    private String serverPort;

    // Added baseUrl property to avoid hard-coded localhost
    @Value("${discord.upload.base-url}")
    private String baseUrl;

    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("File size exceeds the maximum limit of 5MB");
            }

            // Validate file type
            String contentType = file.getContentType();
            if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Only JPEG and PNG files are allowed");
            }

            // Create directory structure based on date
            LocalDate today = LocalDate.now();
            String datePath = String.format("%d/%02d/%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
            Path uploadPath = Paths.get(UPLOAD_DIR, datePath);
            Files.createDirectories(uploadPath);

            // Save file with a unique name
            // Improved filename handling to avoid crashes when the file has no extension.
            String originalFilename = file.getOriginalFilename();
            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                // Fallback: derive extension from content type
                if ("image/jpeg".equals(file.getContentType())) {
                    extension = ".jpg";
                } else if ("image/png".equals(file.getContentType())) {
                    extension = ".png";
                }
            }

            String uniqueFilename = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            file.transferTo(filePath);

            // Return file URL
            // Updated fileUrl to use baseUrl from application.properties
            String fileUrl = String.format("%s/%s/%s/%s", baseUrl, UPLOAD_DIR, datePath, uniqueFilename);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error occurred"));
        }
    }
}
