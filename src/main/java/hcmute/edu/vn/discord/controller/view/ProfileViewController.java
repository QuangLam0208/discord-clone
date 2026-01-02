package hcmute.edu.vn.discord.controller.view;

import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
@RequiredArgsConstructor
public class ProfileViewController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/profile")
    public String profilePage(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";
        User currentUser = userService.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";
        model.addAttribute("user", currentUser);
        return "profile";
    }

    @PostMapping("/profile/avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";
        User user = userService.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (file.isEmpty()) return "redirect:/profile?error=empty";

        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path uploadDir = Paths.get("target/classes/static/uploads/");
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            user.setAvatarUrl("/uploads/" + fileName);
            userRepository.save(user);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/profile?error=server";
        }
        return "redirect:/profile?success";
    }
}