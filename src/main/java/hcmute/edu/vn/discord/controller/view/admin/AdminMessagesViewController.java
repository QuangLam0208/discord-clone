package hcmute.edu.vn.discord.controller.view.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/messages")
public class AdminMessagesViewController {

    @GetMapping({"", "/"})
    public String messagesSpa() {
        return "redirect:/admin#messages";
    }
}