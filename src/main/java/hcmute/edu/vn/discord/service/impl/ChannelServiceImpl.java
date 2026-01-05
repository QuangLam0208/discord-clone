package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.service.ChannelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final hcmute.edu.vn.discord.service.AuditLogService auditLogService;

    @Override
    @Transactional
    public Channel createChannel(Long serverId, String name, ChannelType type, Long categoryId,
                                 Boolean isPrivate, String createdByUsername) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));

        Channel channel = new Channel();
        channel.setName(name);
        channel.setType(type != null ? type : ChannelType.TEXT);
        channel.setServer(server);
        channel.setIsPrivate(isPrivate != null ? isPrivate : false);

        if (categoryId != null && categoryId > 0) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));

            if (!category.getServer().getId().equals(serverId)) {
                throw new IllegalArgumentException("Category does not belong to this server");
            }
            channel.setCategory(category);
        }

        // Auto-add creator to allowedMembers for private channels
        User creator = null;
        if (createdByUsername != null && !createdByUsername.isBlank()) {
            creator = userRepository.findByUsername(createdByUsername).orElse(null);
        }

        if (Boolean.TRUE.equals(channel.getIsPrivate()) && creator != null) {
            ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, creator.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Creator is not a member of this server"));
            channel.getAllowedMembers().add(member);
        }

        Channel saved = channelRepository.save(channel);

        // Audit Log
        if (creator == null) {
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            creator = userRepository.findByUsername(username).orElse(null);
        }
        auditLogService.logAction(server, creator, hcmute.edu.vn.discord.common.AuditLogAction.CHANNEL_CREATE,
                saved.getId().toString(), "CHANNEL", "Tạo kênh: " + saved.getName());

        return saved;
    }

    @Override
    @Transactional
    public Channel updateChannel(Long channelId, String name, String description, Long categoryId, Boolean isPrivate) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));

        String oldName = channel.getName();
        String oldDesc = channel.getDescription();
        Boolean oldPrivate = channel.getIsPrivate();

        if (name != null && !name.isBlank())
            channel.setName(name);
        if (description != null)
            channel.setDescription(description);
        if (isPrivate != null)
            channel.setIsPrivate(isPrivate);

        if (categoryId != null) {
            if (categoryId <= 0) {
                channel.setCategory(null);
            } else {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new EntityNotFoundException("Category not found"));
                if (!category.getServer().getId().equals(channel.getServer().getId())) {
                    throw new IllegalArgumentException("Category mismatch");
                }
                channel.setCategory(category);
            }
        }
        Channel updated = channelRepository.save(channel);

        // Audit Log
        try {
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User actor = userRepository.findByUsername(username).orElse(null);

            StringBuilder changes = new StringBuilder();
            if (!java.util.Objects.equals(oldName, updated.getName())) {
                changes.append("Tên: ").append(oldName).append(" -> ").append(updated.getName()).append("; ");
            }
            if (!java.util.Objects.equals(oldDesc, updated.getDescription())) {
                changes.append("Mô tả: thay đổi; ");
            }
            if (!java.util.Objects.equals(oldPrivate, updated.getIsPrivate())) {
                changes.append("Riêng tư: ").append(oldPrivate).append(" -> ").append(updated.getIsPrivate())
                        .append("; ");
            }

            if (changes.length() > 0) {
                auditLogService.logAction(updated.getServer(), actor,
                        hcmute.edu.vn.discord.common.AuditLogAction.CHANNEL_UPDATE,
                        updated.getId().toString(), "CHANNEL", changes.toString());
            }
        } catch (Exception e) {
        }

        return updated;
    }

    @Override
    @Transactional
    public void deleteChannel(Long channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        Server server = channel.getServer();
        String channelName = channel.getName();
        String channelIdStr = channel.getId().toString();

        channelRepository.deleteById(channelId);

        try {
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            User actor = userRepository.findByUsername(username).orElse(null);

            auditLogService.logAction(server, actor, hcmute.edu.vn.discord.common.AuditLogAction.CHANNEL_DELETE,
                    channelIdStr, "CHANNEL", "Xóa kênh: " + channelName);
        } catch (Exception e) {
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Channel> getChannelsByServer(Long serverId) {
        // Gọi Repository (findByServerIdOrderByIdAsc hoặc findByServerId đều được)
        return channelRepository.findByServerIdOrderByIdAsc(serverId);
    }

    @Override
    @Transactional(readOnly = true)
    public Channel getChannelById(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
    }
}