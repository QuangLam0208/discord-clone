package hcmute.edu.vn.discord.controller.view.admin;

import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/servers")
@PreAuthorize("hasAnyRole('ADMIN')")
@RequiredArgsConstructor
public class AdminServersViewController {

    private final ServerRepository serverRepository;
    private final UserService userService;

    @GetMapping({"", "/"})
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String keyword) {
        int size = 10;
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Server> serverPage = keyword.isBlank()
                ? serverRepository.findAll(pageable)
                : serverRepository.findByNameContainingIgnoreCase(keyword, pageable);

        model.addAttribute("servers", serverPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", serverPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        return "admin/servers";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long id) {
        serverRepository.deleteById(id);
        return "redirect:/admin/servers";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam Long serverId, @RequestParam String newOwnerUsername) {
        Server server = serverRepository.findById(serverId).orElse(null);
        User newOwner = userService.findByUsername(newOwnerUsername).orElse(null);
        if (server != null && newOwner != null) {
            server.setOwner(newOwner);
            serverRepository.save(server);
        }
        return "redirect:/admin/servers";
    }
}