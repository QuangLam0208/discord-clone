package hcmute.edu.vn.discord.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    // Trả về trang chủ, để frontend tự kiểm tra token và điều hướng
    @GetMapping("/")
    public String home() {
        return "home"; // templates/home.html
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
}