package hcmute.edu.vn.discord.controller.api;

import hcmute.edu.vn.discord.dto.response.ErrorResponse;
import hcmute.edu.vn.discord.dto.response.UploadResponse;
import hcmute.edu.vn.discord.service.FileValidationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final String TYPE_JPEG = "image/jpeg";
    private static final String TYPE_PNG = "image/png";
    private static final String TYPE_WEBP = "image/webp";

    private static final Set<String> ALLOWED_TYPES = Set.of(TYPE_JPEG, TYPE_PNG, TYPE_WEBP);

    @Value("${discord.upload.base-url}")
    private String baseUrl;

    @Value("${discord.upload.dir:uploads}")
    private String uploadDir;

    private final FileValidationService fileValidationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication != null ? authentication.getName() : "anonymous";
            log.info("Yêu cầu upload từ user={} fileName={} size={}", username, file.getOriginalFilename(),
                    file.getSize());

            if (file.isEmpty()) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "File rỗng");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "Kích thước file vượt quá 10MB");
            }

            // Canonicalize content type (ví dụ image/jpg -> image/jpeg)
            String declared = canonicalizeContentType(file.getContentType());

            // Dùng Tika để detect
            byte[] fileBytes = file.getBytes();
            String detected = fileValidationService.detectContentType(fileBytes);

            // Chấp nhận nếu detected ∈ whitelist (ưu tiên security)
            boolean isAllowed = ALLOWED_TYPES.contains(detected);

            // Fallback: Nếu Tika không nhận diện được (octet-stream/text), tin tưởng
            // declared type
            if (!isAllowed) {
                if (ALLOWED_TYPES.contains(declared) &&
                        ("application/octet-stream".equals(detected) || "text/plain".equals(detected))) {
                    log.warn("Tika failed (detected={}), falling back to declared={}", detected, declared);
                    detected = declared; // Trust declared
                    isAllowed = true;
                }
            }

            if (!isAllowed) {
                return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Chỉ cho phép JPEG/PNG/WebP (Detected: " + detected + ")");
            }

            // Nếu declared khác detected (và cả 2 đều hợp lệ), vẫn cho qua nhưng log cảnh
            // báo
            if (declared != null && !declared.equals(detected)) {
                log.warn("Declared content type '{}' không khớp với detected '{}'", declared, detected);
            }

            // Validate dimensions (cần ImageIO plugin cho WEBP)
            try {
                fileValidationService.validateImageDimensions(fileBytes, detected);
            } catch (IllegalArgumentException e) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
            }

            // Xác định phần mở rộng từ loại nội dung
            String extension = switch (detected) {
                case TYPE_JPEG -> ".jpg";
                case TYPE_PNG -> ".png";
                case TYPE_WEBP -> ".webp";
                default -> ".bin";
            };

            // Tạo đường dẫn thư mục theo ngày
            LocalDate today = LocalDate.now();
            String datePath = String.format("%d/%02d/%02d", today.getYear(), today.getMonthValue(),
                    today.getDayOfMonth());

            // Sử dụng ĐƯỜNG DẪN TUYỆT ĐỐI và BÌNH THƯỜNG HÓA
            Path baseUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path uploadPath = baseUploadPath.resolve(datePath).normalize();

            // Bảo mật: Đảm bảo uploadPath nằm trong baseUploadPath
            if (!uploadPath.startsWith(baseUploadPath)) {
                log.error("Traversal path: {}", uploadPath);
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Đường dẫn upload không hợp lệ");
            }

            // Tạo thư mục nếu chưa tồn tại
            Files.createDirectories(uploadPath);

            // Tạo tên file duy nhất
            String uniqueFilename = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(uniqueFilename).normalize();

            // Bảo mật: Kiểm tra đường dẫn cuối cùng
            if (!filePath.startsWith(baseUploadPath)) {
                log.error("Traversal filename: {}", filePath);
                return buildErrorResponse(HttpStatus.BAD_REQUEST, "Tên file không hợp lệ");
            }

            file.transferTo(filePath);
            log.info("File đã được lưu tại: {}", filePath.toAbsolutePath());

            // 5. TẠO URL PHẢN HỒI
            String fileUrl = String.format("%s/files/%s/%s", baseUrl, datePath, uniqueFilename);

            UploadResponse response = UploadResponse.builder()
                    .url(fileUrl)
                    .originalFilename(file.getOriginalFilename())
                    .filename(uniqueFilename)
                    .contentType(detected) // dùng loại đã detect
                    .size(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(username)
                    .build();

            log.info("File đã được upload thành công bởi {}: {}", username, fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Upload IO error: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể upload file do lỗi IO");
        } catch (Exception e) {
            log.error("Unexpected upload error", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi không mong muốn");
        }
    }

    private String canonicalizeContentType(String contentType) {
        if (contentType == null)
            return null;
        String ct = contentType.trim().toLowerCase();
        if (ct.equals("image/jpg"))
            return TYPE_JPEG;
        if (ct.equals("image/x-png"))
            return TYPE_PNG; // một số trình duyệt cũ
        return ct;
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        ErrorResponse error = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message,
                "/api/upload");
        return ResponseEntity.status(status).body(error);
    }
}