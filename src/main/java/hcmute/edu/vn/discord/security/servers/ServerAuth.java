package hcmute.edu.vn.discord.security.servers;

import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.entity.mongo.Message;
import hcmute.edu.vn.discord.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component("serverAuth")
@RequiredArgsConstructor
@Slf4j
public class ServerAuth {

    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final CategoryRepository categoryRepository;

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    private ServerMember requireMember(Long serverId, String username) {
        User user = requireUser(username);
        // Nạp sẵn roles + permissions để tránh LazyInitializationException
        return serverMemberRepository.findWithRolesByServerIdAndUserId(serverId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên server này"));
    }

    public boolean isMember(Long serverId, String username) {
        User user = requireUser(username);
        return serverMemberRepository.existsByServerIdAndUserId(serverId, user.getId());
    }

    public boolean isOwner(Long serverId, String username) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));
        User user = requireUser(username);
        boolean owner = server.getOwner() != null && Objects.equals(server.getOwner().getId(), user.getId());
        return owner;
    }

    // Dùng query JOIN để lấy permission codes, tránh truy cập collection LAZY
    public boolean has(Long serverId, String username, Set<String> codes) {
        Set<String> userPerms = serverMemberRepository.findPermissionCodesByServerIdAndUsername(serverId, username);
        for (String c : userPerms) {
            if (codes.contains(c)) return true;
        }
        return false;
    }

    private boolean isServerFrozen(Long serverId) {
        Server s = serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));
        return s.getStatus() == ServerStatus.FREEZE;
    }

    private boolean isServerAdmin(Long serverId, String username) {
        return has(serverId, username, Set.of(EPermission.ADMIN.getCode()));
    }

    // Xem kênh: FREEZE không chặn xem
    public boolean canViewChannel(Long channelId, String username) {
        User user = requireUser(username);
        // Nạp sẵn access lists + server để tránh LAZY
        Channel channel = channelRepository.findWithAccessListsById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        Long serverId = channel.getServer().getId();

        if (isOwner(serverId, username))
            return true;

        ServerMember member = requireMember(serverId, username);
        if (Boolean.TRUE.equals(member.getIsBanned())) {
            throw new AccessDeniedException("Bạn đã bị cấm khỏi server này.");
        }

        if (!Boolean.TRUE.equals(channel.getIsPrivate())) {
            return has(serverId, username, Set.of(EPermission.VIEW_CHANNELS.getCode()));
        }

        boolean isAdmin = isServerAdmin(serverId, username);
        if (isAdmin) return true;

        boolean userAllowed = channel.getAllowedMembers() != null && channel.getAllowedMembers().contains(member);
        boolean roleAllowed = channel.getAllowedRoles() != null && member.getRoles().stream()
                .anyMatch(r -> channel.getAllowedRoles().contains(r));

        return userAllowed || roleAllowed;
    }

    // FREEZE: OWNER hoặc ADMIN; ACTIVE: OWNER hoặc SEND_MESSAGES
    public boolean canSendMessage(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;

        Channel channel = channelRepository.findWithAccessListsById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        Long serverId = channel.getServer().getId();

        if (isServerFrozen(serverId)) {
            return isOwner(serverId, username) || isServerAdmin(serverId, username);
        }

        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(EPermission.SEND_MESSAGES.getCode()));
    }

    public boolean canManageMessages(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Long serverId = channelRepository.findServerIdByChannelId(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        if (isServerFrozen(serverId)) {
            return isOwner(serverId, username) || isServerAdmin(serverId, username);
        }
        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(EPermission.ADMIN.getCode(), EPermission.MANAGE_MESSAGES.getCode()));
    }

    public boolean canReactToMessage(String messageId, String username) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        Long channelId = msg.getChannelId();
        if (!canViewChannel(channelId, username))
            return false;
        Long serverId = channelRepository.findServerIdByChannelId(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        if (isServerFrozen(serverId)) {
            return isOwner(serverId, username) || isServerAdmin(serverId, username);
        }
        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(EPermission.ADD_REACTIONS.getCode(), EPermission.ADMIN.getCode()));
    }

    public boolean canReadMessageHistory(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Long serverId = channelRepository.findServerIdByChannelId(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        return has(serverId, username, Set.of(EPermission.READ_MESSAGE_HISTORY.getCode()));
    }

    public boolean canAttachFiles(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Long serverId = channelRepository.findServerIdByChannelId(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        if (isServerFrozen(serverId)) {
            return isOwner(serverId, username) || isServerAdmin(serverId, username);
        }
        return has(serverId, username, Set.of(EPermission.ATTACH_FILES.getCode(), EPermission.ADMIN.getCode()));
    }

    public boolean canEmbedLink(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Long serverId = channelRepository.findServerIdByChannelId(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        if (isServerFrozen(serverId)) {
            return isOwner(serverId, username) || isServerAdmin(serverId, username);
        }
        return has(serverId, username, Set.of(EPermission.EMBED_LINK.getCode(), EPermission.ADMIN.getCode()));
    }

    public boolean canCreateInvite(Long serverId, String username) {
        if (!isMember(serverId, username))
            return false;
        if (isServerFrozen(serverId)) {
            return isOwner(serverId, username) || isServerAdmin(serverId, username);
        }
        return has(serverId, username, Set.of(EPermission.CREATE_INVITE.getCode(), EPermission.ADMIN.getCode()));
    }

    public boolean canChangeNickname(Long serverId, String username) {
        if (!isMember(serverId, username))
            return false;
        if (isServerFrozen(serverId)) {
            return isOwner(serverId, username) || isServerAdmin(serverId, username);
        }
        return has(serverId, username, Set.of(EPermission.CHANGE_NICKNAME.getCode(), EPermission.ADMIN.getCode()));
    }

    public boolean canMentionEveryone(Long serverId, String username) {
        if (!isMember(serverId, username))
            return false;
        if (isServerFrozen(serverId)) {
            return isOwner(serverId, username) || isServerAdmin(serverId, username);
        }
        return has(serverId, username, Set.of(EPermission.MENTION_EVERYONE_HERE_ALLROLES.getCode(), EPermission.ADMIN.getCode()));
    }

    public Long serverIdOfChannel(Long channelId) {
        return channelRepository.findServerIdByChannelId(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
    }

    public Long channelIdOfMessage(String messageId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        return msg.getChannelId();
    }

    public Long serverIdOfMessage(String messageId) {
        Long channelId = channelIdOfMessage(messageId);
        return serverIdOfChannel(channelId);
    }

    public boolean canEditChannel(Long channelId, String username) {
        Long serverId = serverIdOfChannel(channelId);
        return canManageChannels(serverId, username);
    }

    // Quản lý kênh/role/member giữ nguyên logic FREEZE: admin/owner mới được
    public boolean canManageChannels(Long serverId, String username) {
        if (isServerFrozen(serverId) && !isServerAdmin(serverId, username) && !isOwner(serverId, username)) return false;
        if (isOwner(serverId, username)) return true;
        return has(serverId, username, Set.of(EPermission.ADMIN.getCode(), EPermission.MANAGE_CHANNELS.getCode()));
    }

    public boolean canManageRole(Long serverId, String username) {
        if (isServerFrozen(serverId) && !isServerAdmin(serverId, username) && !isOwner(serverId, username)) return false;
        if (isOwner(serverId, username)) return true;
        return has(serverId, username, Set.of(EPermission.ADMIN.getCode(), EPermission.MANAGE_ROLES.getCode()));
    }

    public boolean canManageMembers(Long serverId, String username) {
        if (isServerFrozen(serverId) && !isServerAdmin(serverId, username) && !isOwner(serverId, username)) return false;
        if (isOwner(serverId, username)) return true;
        return has(serverId, username, Set.of(
                EPermission.ADMIN.getCode(),
                EPermission.MANAGE_SERVER.getCode(),
                EPermission.KICK_APPROVE_REJECT_MEMBERS.getCode(),
                EPermission.BAN_MEMBERS.getCode()));
    }
}