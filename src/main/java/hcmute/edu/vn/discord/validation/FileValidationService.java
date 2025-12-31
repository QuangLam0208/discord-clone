package hcmute.edu.vn.discord.validation;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Service để validate file upload với:
 * - Magic bytes detection (Tika)
 * - Image dimension validation (chống decompression bomb)
 */
@Slf4j
@Service
public class FileValidationService {

    private final Tika tika = new Tika();

    // ✅ Giới hạn kích thước ảnh để chống decompression bomb
    private static final int MAX_IMAGE_WIDTH = 10000;   // 10k pixels
    private static final int MAX_IMAGE_HEIGHT = 10000;  // 10k pixels
    private static final long MAX_IMAGE_PIXELS = 100_000_000L; // 100 megapixels

    /**
     * Validate file type bằng magic bytes (không tin tưởng header).
     *
     * @param fileBytes Binary content của file
     * @param declaredContentType Content-Type từ header
     * @return true nếu match, false nếu không
     * @throws IOException nếu không đọc được file
     */
    public boolean validateFileType(byte[] fileBytes, String declaredContentType) throws IOException {
        // Detect actual content type từ magic bytes
        String detectedType = tika.detect(fileBytes);

        log.debug("Declared content type: {}, Detected content type: {}", declaredContentType, detectedType);

        // So sánh với content type khai báo
        if (!detectedType.equals(declaredContentType)) {
            log.warn("Content type mismatch! Declared: {}, Detected: {}", declaredContentType, detectedType);
            return false;
        }

        return true;
    }

    /**
     * Validate ảnh để chống decompression bomb.
     *
     * Kiểm tra:
     * - Width và height không vượt quá giới hạn
     * - Tổng số pixels không vượt quá giới hạn
     *
     * @param fileBytes Binary content của ảnh
     * @param contentType Content type (image/jpeg hoặc image/png)
     * @throws IOException nếu không đọc được ảnh
     * @throws IllegalArgumentException nếu ảnh quá lớn (decompression bomb)
     */
    public void validateImageDimensions(byte[] fileBytes, String contentType)
            throws IOException, IllegalArgumentException {

        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)) {

            if (iis == null) {
                throw new IOException("Không thể tạo ImageInputStream");
            }

            // Lấy ImageReader phù hợp với content type
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                throw new IOException("Không tìm thấy ImageReader cho content type: " + contentType);
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(iis, true, true);

                // ✅ Đọc metadata KHÔNG load toàn bộ ảnh vào memory
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long totalPixels = (long) width * height;

                log.debug("Image dimensions: {}x{} ({} pixels)", width, height, totalPixels);

                // ✅ Validate dimensions
                if (width > MAX_IMAGE_WIDTH) {
                    throw new IllegalArgumentException(
                            String.format("Image width %d vượt quá giới hạn %d pixels", width, MAX_IMAGE_WIDTH)
                    );
                }

                if (height > MAX_IMAGE_HEIGHT) {
                    throw new IllegalArgumentException(
                            String.format("Image height %d vượt quá giới hạn %d pixels", height, MAX_IMAGE_HEIGHT)
                    );
                }

                if (totalPixels > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException(
                            String.format("Image size %d pixels vượt quá giới hạn %d pixels", totalPixels, MAX_IMAGE_PIXELS)
                    );
                }

                log.info("Image validation passed: {}x{}", width, height);

            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Kiểm tra magic bytes của JPEG.
     *
     * JPEG magic bytes: FF D8 FF
     */
    public boolean isJPEG(byte[] fileBytes) {
        if (fileBytes.length < 3) {
            return false;
        }
        return fileBytes[0] == (byte) 0xFF
                && fileBytes[1] == (byte) 0xD8
                && fileBytes[2] == (byte) 0xFF;
    }

    /**
     * Kiểm tra magic bytes của PNG.
     *
     * PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
     */
    public boolean isPNG(byte[] fileBytes) {
        if (fileBytes.length < 8) {
            return false;
        }
        return fileBytes[0] == (byte) 0x89
                && fileBytes[1] == (byte) 0x50
                && fileBytes[2] == (byte) 0x4E
                && fileBytes[3] == (byte) 0x47
                && fileBytes[4] == (byte) 0x0D
                && fileBytes[5] == (byte) 0x0A
                && fileBytes[6] == (byte) 0x1A
                && fileBytes[7] == (byte) 0x0A;
    }
}
