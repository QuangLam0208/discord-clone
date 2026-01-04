package hcmute.edu.vn.discord.controller.view.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminMessagesFragmentController {

    // Trả về template: classpath:/templates/admin/partials/messages_channel.html
    @GetMapping("/admin/_messages_channel")
    public String messagesChannel() {
        return "admin/partials/messages_channel";
    }

    // Trả về template: classpath:/templates/admin/partials/messages_direct.html
    @GetMapping("/admin/_messages_direct")
    public String messagesDirect() {
        return "admin/partials/messages_direct";
    }
}