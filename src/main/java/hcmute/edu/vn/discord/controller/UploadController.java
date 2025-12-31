package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.response.ErrorResponse;
import hcmute.edu.vn.discord.dto.response.UploadResponse;
import hcmute.edu.vn.discord.service.FileValidationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller xử lý upload file (ảnh).
 *
 * Endpoints:
 * - POST /api/upload:  Upload file với JWT authentication
 *
 * Features:
 * - Validate file type (JPEG, PNG only)
 * - Validate file size (max 10MB)
 * - Generate unique filename with UUID
 * - Store in date-based directory structure
 * - Return DTO with file metadata
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    // ✅ Giới hạn kích thước file: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // ✅ Các content type được phép
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";
    private static final String CONTENT_TYPE_PNG = "image/png";

    @Value("${discord.upload.base-url}")
    private String baseUrl;

    @Value("${discord.upload.dir:uploads}")
    private String uploadDir;

    private final FileValidationService fileValidationService;

    /**
     * Upload file endpoint.
     *
     * @param file MultipartFile to upload
     * @param authentication Current authenticated user
     * @return UploadResponse DTO with file metadata
     */
    @PostMapping
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {
            // ═══════════════════════════════════════════════════════
            // 1. LẤY TÊN NGƯỜI DÙNG ĐỂ AUDIT
            // ═══════════════════════════════════════════════════════
            String username = authentication != null
                    ? authentication.getName()
                    : "anonymous";

            log.info("Yêu cầu upload từ người dùng: {}, tên file gốc: {}, kích thước: {} bytes",
                    username, file.getOriginalFilename(), file.getSize());

            // ═══════════════════════════════════════════════════════
            // 2. KIỂM TRA FILE
            // ═══════════════════════════════════════════════════════

            // 2.1 Kiểm tra file rỗng
            if (file.isEmpty()) {
                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "File rỗng"
                );
            }

            // 2.2 Kiểm tra kích thước
            if (file.getSize() > MAX_FILE_SIZE) {
                return buildErrorResponse(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "Kích thước file vượt quá giới hạn tối đa 10MB"
                );
            }

            // 2.3 Kiểm tra loại nội dung
            String contentType = file.getContentType();
            if (!CONTENT_TYPE_JPEG.equals(contentType)
                    && !CONTENT_TYPE_PNG.equals(contentType)) {
                return buildErrorResponse(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Chỉ cho phép file JPEG và PNG"
                );
            }

            byte[] fileBytes = file.getBytes();
            if (!fileValidationService.validateFileType(fileBytes, contentType)) {
                return buildErrorResponse(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Nội dung file không khớp với loại nội dung đã khai báo"
                );
            }

            try {
                fileValidationService.validateImageDimensions(fileBytes, contentType);
            } catch (IllegalArgumentException e) {
                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        e.getMessage()
                );
            }

            // ═══════════════════════════════════════════════════════
            // 3. TẠO TÊN FILE & ĐƯỜNG DẪN
            // ═══════════════════════════════════════════════════════

            // 3.1 Xác định phần mở rộng từ loại nội dung
            String extension = switch (contentType) {
                case CONTENT_TYPE_JPEG -> ".jpg";
                case CONTENT_TYPE_PNG -> ".png";
                default -> throw new IllegalStateException(
                        "Loại nội dung không hợp lệ: " + contentType
                );
            };

            // 3.2 Tạo đường dẫn thư mục theo ngày
            LocalDate today = LocalDate.now();
            String datePath = String.format("%d/%02d/%02d",
                    today.getYear(),
                    today.getMonthValue(),
                    today.getDayOfMonth()
            );

            // ✅ 3.3 Sử dụng ĐƯỜNG DẪN TUYỆT ĐỐI và BÌNH THƯỜNG HÓA
            Path baseUploadPath = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Path uploadPath = baseUploadPath.resolve(datePath)
                    .normalize();

            // ✅ 3.4 Bảo mật: Đảm bảo uploadPath nằm trong baseUploadPath
            if (!uploadPath.startsWith(baseUploadPath)) {
                log.error("Phát hiện cố gắng traversal path: {}", uploadPath);
                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Đường dẫn upload không hợp lệ"
                );
            }

            // 3.5 Tạo thư mục nếu chưa tồn tại
            Files.createDirectories(uploadPath);

            // 3.6 Tạo tên file duy nhất
            String uniqueFilename = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(uniqueFilename).normalize();

            // ✅ 3.7 Bảo mật: Kiểm tra đường dẫn cuối cùng
            if (!filePath.startsWith(baseUploadPath)) {
                log.error("Phát hiện cố gắng traversal path trong tên file: {}", filePath);
                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Tên file không hợp lệ"
                );
            }

            // ═══════════════════════════════════════════════════════
            // 4. LƯU FILE
            // ═══════════════════════════════════════════════════════
            file.transferTo(filePath);
            log.info("File đã được lưu tại: {}", filePath.toAbsolutePath());

            // ═══════════════════════════════════════════════════════
            // 5. TẠO URL PHẢN HỒI
            // ═══════════════════════════════════════════════════════
            String fileUrl = String.format("%s/files/%s/%s",
                    baseUrl,
                    datePath,
                    uniqueFilename
            );

            // ═══════════════════════════════════════════════════════
            // 6. TẠO & TRẢ VỀ DTO
            // ═══════════════════════════════════════════════════════
            UploadResponse response = UploadResponse.builder()
                    .url(fileUrl)
                    .originalFilename(file.getOriginalFilename())
                    .filename(uniqueFilename)
                    .contentType(contentType)
                    .size(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .uploadedBy(username)
                    .build();

            log.info("File đã được upload thành công bởi {}: {}", username, fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Không thể upload file cho người dùng {}: {}",
                    authentication != null ? authentication.getName() : "anonymous",
                    e.getMessage(), e);

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể upload file do lỗi hệ thống"
            );

        } catch (Exception e) {
            log.error("Lỗi không mong muốn trong quá trình upload", e);

            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Đã xảy ra lỗi không mong muốn"
            );
        }
    }

    /**
     * Phương thức trợ giúp để tạo phản hồi lỗi với DTO ErrorResponse.
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                "/api/upload"
        );

        return ResponseEntity.status(status).body(error);
    }
}
