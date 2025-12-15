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
    private ServerRoleRepository serverRoleRepository; // Inject thêm repository này

    @Override
    public ServerMember addMemberToServer(Long serverId, Long userId) {
        if (isMember(serverId, userId)) {
            return null;
        }

        Server server = serverRepository.findById(serverId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);

        if (server != null && user != null) {
            ServerMember newMember = new ServerMember();
            newMember.setServer(server);
            newMember.setUser(user);
            newMember.setJoinedAt(LocalDateTime.now());
            newMember.setIsBanned(false);
            newMember.setNickname(user.getUsername());

            newMember.setRoles(new ArrayList<>());

            // Tìm role mặc định tên "Member" (hoặc tên khác tùy bạn đặt trong DB)
            Optional<ServerRole> defaultRole = serverRoleRepository.findByServerIdAndName(serverId, "Member");

            // Nếu tìm thấy thì thêm vào list
            defaultRole.ifPresent(role -> newMember.getRoles().add(role));

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