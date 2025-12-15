package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.ServerRoleRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.ServerMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ServerMemberServiceImpl implements ServerMemberService {

    @Autowired
    private ServerMemberRepository serverMemberRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServerRoleRepository serverRoleRepository;

    @Override
    public ServerMember addMemberToServer(Long serverId, Long userId) {
        // 1. Kiểm tra xem đã là thành viên chưa
        if (isMember(serverId, userId)) {
            return null;
        }

        // 2. Lấy thông tin Server và User
        Server server = serverRepository.findById(serverId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (server != null && user != null) {
            ServerMember newMember = new ServerMember();
            newMember.setServer(server);
            newMember.setUser(user);

            // A. Set các trường cơ bản còn thiếu
            newMember.setJoinedAt(LocalDateTime.now()); // Thời gian tham gia
            newMember.setIsBanned(false);               // Mặc định không bị ban
            newMember.setNickname(user.getUsername());  // Mặc định nickname giống username

            // B. Xử lý Role (Thay cho setRole("MEMBER"))
            // Khởi tạo danh sách role rỗng để tránh NullPointerException
            newMember.setRoles(new ArrayList<>());

            // Tìm role mặc định có tên là "Member" của server này
            Optional<ServerRole> defaultRole = serverRoleRepository.findByServerIdAndName(serverId, "Member");

            // Nếu tìm thấy role "Member" thì gán cho user mới
            defaultRole.ifPresent(role -> newMember.getRoles().add(role));

            // Lưu vào DB
            return serverMemberRepository.save(newMember);
        }
        return null;
    }

    @Override
    public List<ServerMember> getMembersByServerId(Long serverId) {
        return serverMemberRepository.findByServerId(serverId);
    }

    @Override
    public boolean isMember(Long serverId, Long userId) {
        return serverMemberRepository.existsByServerIdAndUserId(serverId, userId);
    }

    @Override
    public void removeMember(Long serverId, Long userId) {
        Optional<ServerMember> memberOpt = serverMemberRepository.findByServerIdAndUserId(serverId, userId);
        memberOpt.ifPresent(member -> serverMemberRepository.delete(member));
    }
}