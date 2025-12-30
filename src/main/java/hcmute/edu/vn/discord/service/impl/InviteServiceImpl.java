package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Invite;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.InviteRepository;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.InviteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InviteServiceImpl implements InviteService {

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ServerMemberRepository serverMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Invite createInvite(Long serverId, Integer maxUses, Long expireSeconds) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("Server not found"));

        Invite invite = new Invite();
        invite.setServer(server);

        // Tạo mã code ngẫu nhiên (lấy 8 ký tự đầu của UUID)
        invite.setCode(UUID.randomUUID().toString().substring(0, 8));

        invite.setMaxUses(maxUses); // null hoặc 0 nghĩa là không giới hạn
        invite.setUsedCount(0);
        invite.setRevoked(false);

        // Tính thời gian hết hạn
        if (expireSeconds != null && expireSeconds > 0) {
            long expireTimeInMillis = System.currentTimeMillis() + (expireSeconds * 1000);
            invite.setExpiresAt(new Date(expireTimeInMillis));
        } else {
            invite.setExpiresAt(null); // Vĩnh viễn
        }

        return inviteRepository.save(invite);
    }

    @Override
    public Optional<Invite> getInviteByCode(String code) {
        return inviteRepository.findByCode(code);
    }

    @Override
    @Transactional
    public void joinServer(String code) {
        // 1. Kiểm tra tính hợp lệ và tăng lượt sử dụng của Invite
        if (!useInvite(code)) {
            throw new RuntimeException("Mã mời không hợp lệ, đã hết hạn hoặc hết lượt dùng.");
        }

        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invite not found"));

        // 2. Lấy thông tin User hiện tại đang đăng nhập
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Kiểm tra xem người dùng đã là thành viên Server chưa
        if (serverMemberRepository.existsByServerIdAndUserId(invite.getServer().getId(), user.getId())) {
            return; // Đã là thành viên rồi thì không cần xử lý thêm
        }

        // 4. Tạo mới ServerMember
        ServerMember member = new ServerMember();
        member.setServer(invite.getServer());
        member.setUser(user);
        member.setIsBanned(false);
        member.setNickname(user.getDisplayName()); // Mặc định dùng DisplayName của User

        serverMemberRepository.save(member);
    }

    @Override
    public boolean useInvite(String code) {
        Optional<Invite> inviteOpt = inviteRepository.findByCode(code);

        if (inviteOpt.isEmpty()) return false;

        Invite invite = inviteOpt.get();

        // 1. Kiểm tra đã bị thu hồi chưa
        if (Boolean.TRUE.equals(invite.getRevoked())) return false;

        // 2. Kiểm tra hết hạn thời gian
        if (invite.getExpiresAt() != null && invite.getExpiresAt().before(new Date())) {
            return false;
        }

        // 3. Kiểm tra giới hạn lượt dùng
        if (invite.getMaxUses() != null && invite.getMaxUses() > 0) {
            if (invite.getUsedCount() >= invite.getMaxUses()) {
                return false;
            }
        }

        // Tăng lượt dùng và lưu lại
        invite.setUsedCount(invite.getUsedCount() + 1);
        inviteRepository.save(invite);
        return true;
    }

    @Override
    public List<Invite> getInvitesByServer(Long serverId) {
        return inviteRepository.findByServerId(serverId);
    }

    @Override
    public void revokeInvite(Long inviteId) {
        Invite invite = inviteRepository.findById(inviteId).orElse(null);
        if (invite != null) {
            invite.setRevoked(true);
            inviteRepository.save(invite);
        }
    }
}