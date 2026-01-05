package hcmute.edu.vn.discord.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

@Slf4j
@Service
public class FileValidationService {

    private final Tika tika = new Tika();

    // Giới hạn kích thước ảnh để chống decompression bomb
    private static final int MAX_IMAGE_WIDTH = 10000; // 10kpx
    private static final int MAX_IMAGE_HEIGHT = 10000;
    private static final long MAX_IMAGE_PIXELS = 100_000_000L; // 100 megapx

    public String detectContentType(byte[] fileBytes) {
        return tika.detect(fileBytes);
    }

    public void validateImageDimensions(byte[] fileBytes, String contentType)
            throws IOException, IllegalArgumentException {

        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
                ImageInputStream iis = ImageIO.createImageInputStream(bais)) {

            if (iis == null) throw new IOException("Không thể tạo ImageInputStream");

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                // Với WEBP nếu thiếu plugin sẽ không có reader -> log warn và SKIP validation thay vì error
                log.warn("Không tìm thấy ImageReader cho content type: {}. Bỏ qua kiểm tra kích thước.", contentType);
                return;
            }
            ImageReader reader = readers.next();
            try {
                // chỉ đọc header ảnh (metadata), không load full ảnh
                reader.setInput(iis, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long totalPixels = (long) width * height;

                if (width > MAX_IMAGE_WIDTH) {
                    throw new IllegalArgumentException(
                            String.format("Image width %d vượt quá %d pixels", width, MAX_IMAGE_WIDTH));
                }
                if (height > MAX_IMAGE_HEIGHT) {
                    throw new IllegalArgumentException(
                            String.format("Image height %d vượt quá %d pixels", height, MAX_IMAGE_HEIGHT));
                }
                if (totalPixels > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException(
                            String.format("Image size %d pixels vượt quá %d pixels", totalPixels, MAX_IMAGE_PIXELS));
                }
                log.info("Image validation passed: {}x{}", width, height);
            } finally {
                reader.dispose();
            }
        }
    }
}
