package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.MessageRequest;
import hcmute.edu.vn.discord.dto.response.MessageResponse;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.mongo.Message;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;


    @Override
    public Message sendMessage(Message message) {
        message.setCreatedAt(new Date());
        message.setDeleted(false);
        return messageRepository.save(message);
    }

    @Override
    public MessageResponse createMessage(Long channelId, String username, MessageRequest request) {
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user: " + username));

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy kênh với ID: " + channelId));

        checkChannelAccess(sender, channel);
        ensurePermission(sender.getId(), channel.getServer().getId(), EPermission.SEND_MESSAGES);

        boolean hasContent = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasAttachments = request.getAttachments() != null && !request.getAttachments().isEmpty();

        if (!hasContent && !hasAttachments) {
            throw new IllegalArgumentException("Tin nhắn phải có nội dung hoặc ảnh đính kèm");
        }

        Message message = new Message();
        message.setChannelId(channelId);
        message.setSenderId(sender.getId());
        message.setContent(request.getContent() != null ? request.getContent().trim() : null);
        message.setReplyToId(request.getReplyToId()); // Lưu ID tin nhắn được reply
        message.setAttachments(request.getAttachments() != null ? request.getAttachments() : new ArrayList<>());
        message.setCreatedAt(new Date());
        message.setDeleted(false);
        message.setIsEdited(false);

        Message saved = messageRepository.save(message);
        return mapToResponse(saved, sender);
    }

    @Override
    public List<MessageResponse> getMessagesByChannel(Long channelId, String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + channelId));

        checkChannelAccess(user, channel);

        return messageRepository.findByChannelId(channelId, pageable)
                .stream()
                .map(msg -> {
                    User sender = userRepository.findById(msg.getSenderId()).orElse(null);
                    return mapToResponse(msg, sender);
                })
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponse editMessage(String messageId, String username, MessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tin nhắn với ID: " + messageId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user: " + username));

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung chỉnh sửa không được để trống");
        }

        if (!message.getSenderId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa tin nhắn này");
        }

        if (Boolean.TRUE.equals(message.getDeleted())) {
            throw new IllegalArgumentException("Không thể chỉnh sửa tin nhắn đã bị xóa");
        }

        message.setContent(request.getContent().trim());
        message.setIsEdited(true); // Đánh dấu đã sửa
        return mapToResponse(messageRepository.save(message), user);
    }

    @Override
    public void deleteMessage(String messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tin nhắn với ID: " + messageId));

        User requester = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy user: " + username));

        boolean isAuthor = message.getSenderId().equals(requester.getId());
        boolean isServerOwner = false;
        boolean hasManageMessages = false;

        if (!isAuthor) {
            Channel channel = channelRepository.findById(message.getChannelId())
                    .orElseThrow(() -> new EntityNotFoundException("Channel not found"));

            if (channel.getServer().getOwner().getId().equals(requester.getId())) {
                isServerOwner = true;
            }

            ServerMember member = serverMemberRepository.findByServerIdAndUserId(channel.getServer().getId(), requester.getId()).orElse(null);
            if (member != null) {
                hasManageMessages = member.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .anyMatch(p -> p.getCode().equals(EPermission.MANAGE_MESSAGES.getCode())
                                || p.getCode().equals(EPermission.ADMIN.getCode()));
            }
        }

        if (!isAuthor && !isServerOwner && !hasManageMessages) {
            throw new AccessDeniedException("Bạn không có quyền xóa tin nhắn này");
        }

        message.setDeleted(true);
        message.setContent(null);
        message.setAttachments(null);
        messageRepository.save(message);
    }

    @Override
    public void addReaction(String messageId, String username, String emoji) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        Channel channel = channelRepository.findById(msg.getChannelId())
                .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + msg.getChannelId()));

        checkChannelAccess(user, channel);
        ensurePermission(user.getId(), channel.getServer().getId(), EPermission.ADD_REACTIONS);

        if (msg.getReactions() == null) {
            msg.setReactions(new ArrayList<>());
        }

        boolean alreadyReacted = msg.getReactions().stream()
                .anyMatch(r -> r.getUserId().equals(user.getId()) && r.getEmoji().equals(emoji));

        if (alreadyReacted) {
            return; // tránh trùng
        }

        Message.Reaction r = new Message.Reaction();
        r.setUserId(user.getId());
        r.setEmoji(emoji);
        msg.getReactions().add(r);
        messageRepository.save(msg);
    }

    @Override
    public void removeReaction(String messageId, String username, String emoji) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        Channel channel = channelRepository.findById(msg.getChannelId())
                .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + msg.getChannelId()));

        checkChannelAccess(user, channel);
        ensurePermission(user.getId(), channel.getServer().getId(), EPermission.ADD_REACTIONS);

        if(msg.getReactions() != null) {
            msg.getReactions().removeIf(r -> r.getUserId().equals(user.getId()) && r.getEmoji().equals(emoji));
            messageRepository.save(msg);
        }
    }

    // --- HELPER: kiểm tra access xem channel và trạng thái ban ---
    private void checkChannelAccess(User user, Channel channel) {
        Long serverId = channel.getServer().getId();

        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn chưa tham gia Server này!"));

        if (Boolean.TRUE.equals(member.getIsBanned())) {
            throw new AccessDeniedException("Bạn đã bị cấm khỏi server này.");
        }

        if (!Boolean.TRUE.equals(channel.getIsPrivate())) {
            return;
        }

        // Private channel
        if (channel.getServer().getOwner() != null
                && channel.getServer().getOwner().getId().equals(user.getId())) {
            return;
        }

        boolean isAdmin = member.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getCode().equals(EPermission.ADMIN.getCode()));
        if (isAdmin) return;

        boolean isUserAllowed = channel.getAllowedMembers().stream()
                .anyMatch(m -> m.getId().equals(member.getId()));

        boolean isRoleAllowed = channel.getAllowedRoles() != null
                && member.getRoles().stream().anyMatch(channel.getAllowedRoles()::contains);

        if (!isUserAllowed && !isRoleAllowed) {
            throw new AccessDeniedException("Đây là kênh riêng tư, bạn không có quyền truy cập.");
        }
    }

    // --- HELPER: yêu cầu permission cụ thể trên server ---
    private void ensurePermission(Long userId, Long serverId, EPermission required) {
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên server này"));

        boolean has = member.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getCode().equals(required.getCode()) || p.getCode().equals(EPermission.ADMIN.getCode()));

        if (!has) {
            throw new AccessDeniedException("Thiếu quyền: " + required.getCode());
        }
    }

    // Helper mapToResponse Xử lý Reply
    private MessageResponse mapToResponse(Message msg, User sender) {
        MessageResponse replyToResponse = null;
        if (msg.getReplyToId() != null) {
            Message originalMsg = messageRepository.findById(msg.getReplyToId()).orElse(null);
            if (originalMsg != null) {
                User originalSender = userRepository.findById(originalMsg.getSenderId()).orElse(null);
                replyToResponse = MessageResponse.builder()
                        .id(originalMsg.getId())
                        .content(Boolean.TRUE.equals(originalMsg.getDeleted()) ? "Tin nhắn đã bị xóa" : originalMsg.getContent())
                        .senderName(originalSender != null ? originalSender.getDisplayName() : "Unknown")
                        .build();
            }
        }

        return MessageResponse.builder()
                .id(msg.getId())
                .channelId(msg.getChannelId())
                .content(Boolean.TRUE.equals(msg.getDeleted()) ? "Tin nhắn đã bị xóa" : msg.getContent())
                .createdAt(msg.getCreatedAt())
                .deleted(Boolean.TRUE.equals(msg.getDeleted()))
                .isEdited(Boolean.TRUE.equals(msg.getIsEdited()))
                .senderId(msg.getSenderId())
                .senderName(sender != null ? sender.getDisplayName() : "Unknown User")
                .senderAvatar(sender != null ? sender.getAvatarUrl() : null)
                .reactions(msg.getReactions())
                .attachments(msg.getAttachments())
                .replyTo(replyToResponse)
                .build();
    }
}