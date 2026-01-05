package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.RegisterRequest;
import hcmute.edu.vn.discord.dto.response.UserDetailResponse;
import hcmute.edu.vn.discord.entity.jpa.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface UserService {
    User registerUser(User user);

    void registerUser(RegisterRequest request);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    boolean existsByEmail(String email);

    UserDetailResponse getUserDetail(Long userId);

    User updateProfile(String username, String displayName, String bio, MultipartFile file);

    // Change user password (encoded)
    void changePassword(Long userId, String newPassword);
}