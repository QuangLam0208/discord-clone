package hcmute.edu.vn.discord.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders(
                        "Content-Type",      // Bắt buộc cho JSON/multipart
                        "Authorization",     // Bắt buộc cho xác thực JWT
                        "Accept",            // Header HTTP tiêu chuẩn
                        "Origin",            // Bắt buộc cho CORS
                        "X-Requested-With"   // Header AJAX phổ biến
                )
                .allowCredentials(true)
                .maxAge(3600);  // Cache preflight 1 giờ
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get("uploads").toAbsolutePath().toString(); // Resolve absolute path dynamically
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + absolutePath + "/") // Use absolute path
                .setCachePeriod(604800); // Cache for 7 days
    }
}
