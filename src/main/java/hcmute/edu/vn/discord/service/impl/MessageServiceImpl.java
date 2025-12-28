package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.MessageRequest;
import hcmute.edu.vn.discord.dto.response.MessageResponse;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.mongo.Message;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.repository.MessageRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.MessageService;
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
    private final ServerRepository serverRepository; // Dùng để tìm chủ server

    @Override
    public MessageResponse createMessage(Long channelId, String username, MessageRequest request) {
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate: Không được để trống cả content lẫn ảnh
        if ((request.getContent() == null || request.getContent().trim().isEmpty())
                && (request.getAttachments() == null || request.getAttachments().isEmpty())) {
            throw new IllegalArgumentException("Tin nhắn phải có nội dung hoặc ảnh");
        }

        Message message = new Message();
        message.setChannelId(channelId);
        message.setSenderId(sender.getId());
        message.setContent(request.getContent());
        message.setReplyToId(request.getReplyToId()); // Lưu ID tin nhắn được reply
        message.setAttachments(request.getAttachments() != null ? request.getAttachments() : new ArrayList<>());
        message.setCreatedAt(new Date());

        Message saved = messageRepository.save(message);
        return mapToResponse(saved, sender);
    }

    @Override
    public List<MessageResponse> getMessagesByChannel(Long channelId, Pageable pageable) {
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
                .orElseThrow(() -> new RuntimeException("Message not found"));
        User user = userRepository.findByUsername(username).orElseThrow();

        // 1. Check quyền chính chủ
        if (!message.getSenderId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa tin nhắn này");
        }

        // 2. FIX LOGIC: Chặn sửa tin nhắn đã bị xóa
        if (Boolean.TRUE.equals(message.getDeleted())) {
            throw new IllegalArgumentException("Không thể chỉnh sửa tin nhắn đã bị xóa");
        }

        message.setContent(request.getContent());
        message.setIsEdited(true); // Đánh dấu đã sửa
        return mapToResponse(messageRepository.save(message), user);
    }

    @Override
    public void deleteMessage(String messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        User requester = userRepository.findByUsername(username).orElseThrow();

        // Logic Check Quyền Xóa:
        // 1. Chính chủ tin nhắn
        boolean isAuthor = message.getSenderId().equals(requester.getId());

        // 2. Chủ Server (Admin) cũng được quyền xóa
        boolean isServerOwner = false;
        if (!isAuthor) {
            Channel channel = channelRepository.findById(message.getChannelId())
                    .orElseThrow(() -> new RuntimeException("Channel not found"));
            Server server = serverRepository.findById(channel.getServer().getId()) // Lấy Server từ Channel
                    .orElseThrow(() -> new RuntimeException("Server not found"));

            if (server.getOwner().getId().equals(requester.getId())) {
                isServerOwner = true;
            }
        }

        if (!isAuthor && !isServerOwner) {
            throw new AccessDeniedException("Bạn không có quyền xóa tin nhắn này");
        }

        message.setDeleted(true);
        message.setContent(null); // Xóa nội dung để bảo mật
        message.setAttachments(null); // Xóa ảnh
        messageRepository.save(message);
    }

    // Các hàm reaction giữ nguyên...
    @Override
    public void addReaction(String messageId, String username, String emoji) { /*...*/ }
    @Override
    public void removeReaction(String messageId, String username, String emoji) { /*...*/ }

    // Helper mapToResponse nâng cao (Xử lý Reply)
    private MessageResponse mapToResponse(Message msg, User sender) {
        // Nếu tin nhắn này có reply tin khác -> Tìm tin gốc để hiển thị preview
        MessageResponse replyToResponse = null;
        if (msg.getReplyToId() != null) {
            // Tìm tin nhắn gốc (chấp nhận null nếu tin gốc bị xóa cứng khỏi DB)
            Message originalMsg = messageRepository.findById(msg.getReplyToId()).orElse(null);
            if (originalMsg != null) {
                // Chỉ cần lấy thông tin cơ bản của tin gốc
                User originalSender = userRepository.findById(originalMsg.getSenderId()).orElse(null);
                replyToResponse = MessageResponse.builder()
                        .id(originalMsg.getId())
                        .content(originalMsg.getDeleted() ? "Tin nhắn đã bị xóa" : originalMsg.getContent())
                        .senderName(originalSender != null ? originalSender.getDisplayName() : "Unknown")
                        .build();
            }
        }

        return MessageResponse.builder()
                .id(msg.getId())
                .channelId(msg.getChannelId())
                // Nếu đã xóa thì ẩn nội dung
                .content(Boolean.TRUE.equals(msg.getDeleted()) ? "Tin nhắn đã bị xóa" : msg.getContent())
                .createdAt(msg.getCreatedAt())
                .deleted(Boolean.TRUE.equals(msg.getDeleted()))
                .isEdited(Boolean.TRUE.equals(msg.getIsEdited()))
                .senderId(msg.getSenderId())
                .senderName(sender != null ? sender.getDisplayName() : "Unknown User")
                .senderAvatar(sender != null ? sender.getAvatarUrl() : null) // Giả sử User Entity có avatarUrl
                .reactions(msg.getReactions())
                .attachments(msg.getAttachments()) // Trả về list ảnh
                .replyTo(replyToResponse) // Trả về object reply
                .build();
    }
}