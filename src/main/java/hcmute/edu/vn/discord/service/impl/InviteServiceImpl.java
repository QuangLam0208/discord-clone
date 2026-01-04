package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.InviteRepository;
import hcmute.edu.vn.discord.repository.ServerBanRepository;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import hcmute.edu.vn.discord.repository.ServerRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteServiceImpl implements InviteService {

    private final InviteRepository inviteRepository;
    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final UserRepository userRepository;
    private final ServerRoleRepository serverRoleRepository;
    private final ServerBanRepository serverBanRepository;

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
    public Long joinServer(String code) {
        // 1. Lấy User hiện tại
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông tin người dùng."));

        // 2. Lấy Invite
        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Mã mời không tồn tại hoặc sai mã."));

        // 3. [FIX] Throw lỗi thay vì return im lặng
        if (serverMemberRepository.existsByServerIdAndUserId(invite.getServer().getId(), user.getId())) {
            throw new IllegalStateException("Bạn đã là thành viên của máy chủ này rồi!");
        }

        // 3.1 [SECURITY] Check Ban
        if (serverBanRepository.existsByServerIdAndUserId(invite.getServer().getId(), user.getId())) {
            throw new IllegalStateException("Bạn đã bị cấm tham gia máy chủ này.");
        }

        // 4. Kiểm tra và trừ lượt dùng
        if (!useInvite(code)) {
            throw new IllegalStateException("Mã mời không hợp lệ, đã hết hạn hoặc hết lượt sử dụng.");
        }

        // 5. Tìm Role mặc định (@everyone)
        ServerRole everyoneRole = serverRoleRepository.findByServerIdAndName(invite.getServer().getId(), "@everyone")
                .orElseThrow(() -> new IllegalStateException(
                        "Lỗi hệ thống: Không tìm thấy vai trò mặc định (@everyone) của server."));

        // 6. Tạo Member mới
        ServerMember member = new ServerMember();
        member.setServer(invite.getServer());
        member.setUser(user);
        member.setIsBanned(false);
        member.setNickname(user.getDisplayName());
        member.setJoinedAt(LocalDateTime.now());

        member.setRoles(new HashSet<>(Set.of(everyoneRole)));

        serverMemberRepository.save(member);

        return invite.getServer().getId();
    }

    @Override
    public boolean useInvite(String code) {
        Optional<Invite> inviteOpt = inviteRepository.findByCode(code);

        if (inviteOpt.isEmpty())
            return false;

        Invite invite = inviteOpt.get();

        // 1. Kiểm tra đã bị thu hồi chưa
        if (Boolean.TRUE.equals(invite.getRevoked()))
            return false;

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