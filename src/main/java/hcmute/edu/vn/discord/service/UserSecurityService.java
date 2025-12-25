package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.jpa.UserSecurity;

public interface UserSecurityService {
    // Lấy thông tin security của user
    UserSecurity getByUserId(Long userId);

    // Khởi tạo security cho user mới
    UserSecurity createDefault(User user);

    // Bật 2FA
    void enableTwoFa(Long userId, String secret);

    // Tắt 2FA
    void disableTwoFa(Long userId);

    // Kiểm tra user có bật 2FA không
    boolean isTwoFaEnabled(Long userId);
}
