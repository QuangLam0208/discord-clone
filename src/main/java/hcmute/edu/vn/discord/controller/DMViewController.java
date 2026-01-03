package hcmute.edu.vn.discord.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DMViewController {

    @GetMapping("/dm")
    public String dmPage() {
        return "dm"; // trỏ tới src/main/resources/templates/dm.html
    }
}