package hcmute.edu.vn.discord.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // Dùng @Controller (trả về View), KHÔNG dùng @RestController
public class ViewController {

    // 1. Khi người dùng vào "/", trả về trang chủ (home.html)
    @GetMapping("/")
    public String home() {
        return "home"; // Thymeleaf sẽ tự tìm file templates/home.html
    }

    // 2. Khi người dùng vào "/login", trả về trang login (login.html)
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // 3. Khi người dùng vào "/register", trả về trang đăng ký
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
}