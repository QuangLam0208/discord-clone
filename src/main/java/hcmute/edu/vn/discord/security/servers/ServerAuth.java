package hcmute.edu.vn.discord.security.servers;

import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.Permission;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.mongo.Message;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.repository.MessageRepository;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
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

    // Helper: lấy User theo username
    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    // Helper: lấy ServerMember theo serverId + username
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

    // Kiểm tra có bất kỳ permission code nào trong tập codes
    public boolean has(Long serverId, String username, Set<String> codes) {
        ServerMember member = requireMember(serverId, username);
        return member.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .anyMatch(codes::contains);
    }

    // Quản lý kênh: Owner hoặc ADMIN/MANAGE_CHANNELS
    public boolean canManageChannels(Long serverId, String username) {
        if (isOwner(serverId, username))
            return true;
        return has(serverId, username, Set.of(
                EPermission.ADMIN.getCode(),
                EPermission.MANAGE_CHANNELS.getCode()));
    }

    // Quản lý thành viên: Owner hoặc
    // ADMIN/MANAGE_SERVER/KICK_APPROVE_REJECT_MEMBERS/BAN_MEMBERS
    public boolean canManageMembers(Long serverId, String username) {
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
        return has(serverId, username, Set.of(
                EPermission.ATTACH_FILES.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Nhúng link
    public boolean canEmbedLink(Long channelId, String username) {
        if (!canViewChannel(channelId, username))
            return false;
        Long serverId = serverIdOfChannel(channelId);
        return has(serverId, username, Set.of(
                EPermission.EMBED_LINK.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Tạo lời mời
    public boolean canCreateInvite(Long serverId, String username) {
        if (!isMember(serverId, username))
            return false;
        return has(serverId, username, Set.of(
                EPermission.CREATE_INVITE.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Đổi nickname
    public boolean canChangeNickname(Long serverId, String username) {
        if (!isMember(serverId, username))
            return false;
        return has(serverId, username, Set.of(
                EPermission.CHANGE_NICKNAME.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Mention everyone/here/roles
    public boolean canMentionEveryone(Long serverId, String username) {
        if (!isMember(serverId, username))
            return false;
        return has(serverId, username, Set.of(
                EPermission.MENTION_EVERYONE_HERE_ALLROLES.getCode(),
                EPermission.ADMIN.getCode()));
    }

    // Helper: lấy serverId từ channel
    public Long serverIdOfChannel(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        return channel.getServer().getId();
    }

    // Helper: lấy channelId từ message
    public Long channelIdOfMessage(String messageId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        return msg.getChannelId();
    }

    // Helper: lấy serverId từ message
    public Long serverIdOfMessage(String messageId) {
        Long channelId = channelIdOfMessage(messageId);
        return serverIdOfChannel(channelId);
    }
}