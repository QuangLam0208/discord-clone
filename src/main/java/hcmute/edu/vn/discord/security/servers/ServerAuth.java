package hcmute.edu.vn.discord.security.servers;

import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.entity.mongo.Message;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.repository.MessageRepository;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component("serverAuth")
@RequiredArgsConstructor
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
        return serverMemberRepository.findByServerIdAndUserId(serverId, user.getId())
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
        return server.getOwner() != null && Objects.equals(server.getOwner().getId(), user.getId());
    }

    public boolean has(Long serverId, String username, Set<String> codes) {
        ServerMember member = requireMember(serverId, username);
        return member.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .anyMatch(codes::contains);
    }

    public Long serverIdOfCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        return category.getServer().getId();
    }

    private boolean isServerFrozen(Long serverId) {
        Server s = serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));
        return s.getStatus() == ServerStatus.FREEZE;
    }

    private boolean hasServerAdmin(Long serverId, String username) {
        return has(serverId, username, Set.of(EPermission.ADMIN.getCode()));
    }

    private boolean allowInFreeze(Long serverId, String username) {
        if (!isServerFrozen(serverId)) return true;
        return hasServerAdmin(serverId, username);
    }

    // Quản lý kênh: Owner hoặc ADMIN/MANAGE_CHANNELS
    public boolean canManageChannels(Long serverId, String username) {
        if (!allowInFreeze(serverId, username)) return false;
        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(
                EPermission.ADMIN.getCode(),
                EPermission.MANAGE_CHANNELS.getCode()));
    }

    // Quản lý role: Owner hoặc ADMIN/MANAGE_ROLES
    public boolean canManageRole(Long serverId, String username) {
        if (!allowInFreeze(serverId, username)) return false;
        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(
                EPermission.ADMIN.getCode(),
                EPermission.MANAGE_ROLES.getCode()));
    }

    // Quản lý thành viên: Owner hoặc
    // ADMIN/MANAGE_SERVER/KICK_APPROVE_REJECT_MEMBERS/BAN_MEMBERS
    public boolean canManageMembers(Long serverId, String username) {
        if (!allowInFreeze(serverId, username)) return false;
        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(
                EPermission.ADMIN.getCode(),
                EPermission.MANAGE_SERVER.getCode(),
                EPermission.KICK_APPROVE_REJECT_MEMBERS.getCode(),
                EPermission.BAN_MEMBERS.getCode()));
    }

    // Xem kênh: Owner luôn được xem. Public cần VIEW_CHANNELS; Private cần Admin
    // hoặc nằm trong allowedMembers/allowedRoles
    public boolean canViewChannel(Long channelId, String username) {
        User user = requireUser(username);
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        Long serverId = channel.getServer().getId();

        // Owner bypass
        if (isOwner(serverId, username))
            return true;

        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn chưa tham gia Server này!"));

        if (Boolean.TRUE.equals(member.getIsBanned())) {
            throw new AccessDeniedException("Bạn đã bị cấm khỏi server này.");
        }

        if (!Boolean.TRUE.equals(channel.getIsPrivate())) {
            return has(serverId, username, Set.of(EPermission.VIEW_CHANNELS.getCode()));
        }

        boolean isAdmin = has(serverId, username, Set.of(EPermission.ADMIN.getCode()));
        if (isAdmin)
            return true;

        boolean userAllowed = channel.getAllowedMembers() != null && channel.getAllowedMembers().contains(member);
        boolean roleAllowed = channel.getAllowedRoles() != null && member.getRoles().stream()
                .anyMatch(r -> channel.getAllowedRoles().contains(r));

        return userAllowed || roleAllowed;
    }

    // Gửi tin nhắn: phải xem được kênh + (SEND_MESSAGES hoặc Owner)
    public boolean canSendMessage(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        Long serverId = channel.getServer().getId();

        if (!allowInFreeze(serverId, username)) return false;
        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(EPermission.SEND_MESSAGES.getCode()));
    }

    // Quản lý tin nhắn người khác: Owner hoặc ADMIN/MANAGE_MESSAGES
    public boolean canManageMessages(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        Long serverId = channel.getServer().getId();

        if (!allowInFreeze(serverId, username)) return false;
        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(
                EPermission.ADMIN.getCode(),
                EPermission.MANAGE_MESSAGES.getCode()));
    }

    // Thả/gỡ reaction: phải xem được kênh + ADD_REACTIONS hoặc ADMIN hoặc Owner
    public boolean canReactToMessage(String messageId, String username) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        Long channelId = msg.getChannelId();
        if (!canViewChannel(channelId, username))
            return false;
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        Long serverId = channel.getServer().getId();

        if (!allowInFreeze(serverId, username)) return false;
        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(
                EPermission.ADD_REACTIONS.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Đọc lịch sử: phải xem được kênh + READ_MESSAGE_HISTORY (thường đi kèm
    // VIEW_CHANNELS)
    public boolean canReadMessageHistory(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        Long serverId = channel.getServer().getId();
        return has(serverId, username, Set.of(EPermission.READ_MESSAGE_HISTORY.getCode()));
    }

    // Đính kèm tệp
    public boolean canAttachFiles(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Long serverId = serverIdOfChannel(channelId);
        if (!allowInFreeze(serverId, username)) return false;

        return has(serverId, username, Set.of(
                EPermission.ATTACH_FILES.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Nhúng link
    public boolean canEmbedLink(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Long serverId = serverIdOfChannel(channelId);
        if (!allowInFreeze(serverId, username)) return false;

        return has(serverId, username, Set.of(
                EPermission.EMBED_LINK.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Tạo lời mời
    public boolean canCreateInvite(Long serverId, String username) {
        if (isOwner(serverId, username))
            return true;
        if (!isMember(serverId, username))
            return false;
        if (!allowInFreeze(serverId, username)) return false;

        return has(serverId, username, Set.of(
                EPermission.CREATE_INVITE.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Đổi nickname
    public boolean canChangeNickname(Long serverId, String username) {
        if (!isMember(serverId, username))
            return false;
        if (!allowInFreeze(serverId, username)) return false;

        return has(serverId, username, Set.of(
                EPermission.CHANGE_NICKNAME.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Mention everyone/here/roles
    public boolean canMentionEveryone(Long serverId, String username) {
        if (!isMember(serverId, username))
            return false;
        if (!allowInFreeze(serverId, username)) return false;

        return has(serverId, username, Set.of(
                EPermission.MENTION_EVERYONE_HERE_ALLROLES.getCode(),
                EPermission.ADMIN.getCode()));
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

}