package hcmute.edu.vn.discord.controller.advice;

import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class) // chỉ áp cho các @Controller (view)
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserService userService;

    // Tên biến trong Thymeleaf sẽ là ${currentUser}
    @ModelAttribute("currentUser")
    public User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userService.findByUsername(authentication.getName()).orElse(null);
    }

    // Ví dụ thêm biến tiện ích khác (tùy chọn)
    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }
}