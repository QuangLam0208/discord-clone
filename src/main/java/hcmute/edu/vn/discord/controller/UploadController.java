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
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    // Đã xóa trường serverPort không sử dụng
    // private String serverPort;

    // Thêm thuộc tính baseUrl để tránh hard-code localhost
    @Value("${discord.upload.base-url}")
    private String baseUrl;

    // Thư mục lưu trữ file upload, cấu hình qua application.properties
    @Value("${discord.upload.dir:uploads}")
    private String UPLOAD_DIR;
    // Giới hạn kích thước file tối đa là 5MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 5MB
    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    // Thêm tham số Authentication vào phương thức uploadFile
    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            // Lấy tên người dùng để ghi log/audit
            String username = authentication != null ? authentication.getName() : "anonymous";
            log.info("Yêu cầu upload file từ người dùng: {}", username);

            // 1. Kiểm tra file rỗng
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File rỗng"));
            }

            // 2. Kiểm tra kích thước file
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body(Map.of("error", "Kích thước file vượt quá giới hạn tối đa 10MB"));
            }

            // Kiểm tra loại file
            String contentType = file.getContentType();
            if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(Map.of("error", "Chỉ cho phép file JPEG và PNG"));
            }

            // Xác định phần mở rộng dựa trên loại nội dung
            String extension = switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                default -> throw new IllegalStateException("Loại nội dung không hợp lệ: " + contentType);
            };

            // Tạo cấu trúc thư mục dựa trên ngày hiện tại
            LocalDate today = LocalDate.now();
            String datePath = String.format("%d/%02d/%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
            Path uploadPath = Paths.get(UPLOAD_DIR, datePath);
            Files.createDirectories(uploadPath);

            String uniqueFilename = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            file.transferTo(filePath);

            // Trả về URL của file
            // Cập nhật fileUrl để sử dụng baseUrl từ application.properties
            String fileUrl = String.format("%s/files/%s/%s", baseUrl, datePath, uniqueFilename); // Updated to match WebConfig
            log.info("File được upload thành công bởi {}: {}", username, fileUrl);
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IOException e) {
            log.error("Không thể upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Không thể upload file do lỗi hệ thống, vui lòng thử lại sau."));
        } catch (Exception e) {
            log.error("Lỗi không mong muốn trong quá trình upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Đã xảy ra lỗi không mong muốn"));
        }
    }
}
