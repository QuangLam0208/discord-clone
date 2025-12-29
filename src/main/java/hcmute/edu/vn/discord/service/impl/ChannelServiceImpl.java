package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.ChannelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final ServerMemberRepository serverMemberRepository;

    @Override
    @Transactional
    public Channel createChannel(Channel channel, String creatorUsername) {
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + creatorUsername));

        if (channel.getServer() == null) {
            throw new IllegalArgumentException("Channel must belong to a server");
        }

        Long serverId = channel.getServer().getId();

        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, creator.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải là thành viên của Server này"));

        boolean isOwner = channel.getServer().getOwner() != null
                && channel.getServer().getOwner().getId().equals(creator.getId());

        boolean hasPermission = member.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getCode().equals(EPermission.ADMIN.getCode())
                        || p.getCode().equals(EPermission.MANAGE_CHANNELS.getCode()));

        if (!isOwner && !hasPermission) {
            throw new AccessDeniedException("Bạn không có quyền tạo kênh (Cần quyền MANAGE_CHANNELS)");
        }

        if (channel.getCategory() != null
                && channel.getCategory().getServer() != null
                && !Objects.equals(channel.getCategory().getServer().getId(), serverId)) {
            throw new IllegalArgumentException("Category không thuộc về server này");
        }

        if (Boolean.TRUE.equals(channel.getIsPrivate())) {
            if (channel.getAllowedMembers() == null) {
                channel.setAllowedMembers(new HashSet<>());
            }
            channel.getAllowedMembers().add(member);
        }

        return channelRepository.save(channel);
    }

    @Override
    @Transactional
    public Channel updateChannel(Long id, Channel updatedChannel, String username) {
        Channel existing = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found with id " + id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        checkManageChannelPermission(existing.getServer().getId(), user.getId());

        // name, type, private, category
        existing.setName(updatedChannel.getName());
        existing.setType(updatedChannel.getType());
        existing.setIsPrivate(updatedChannel.getIsPrivate());

        if (updatedChannel.getCategory() != null) {
            if (!Objects.equals(updatedChannel.getCategory().getServer().getId(), existing.getServer().getId())) {
                throw new IllegalArgumentException("Category không thuộc về server của channel");
            }
            existing.setCategory(updatedChannel.getCategory());
        } else {
            existing.setCategory(null);
        }

        // Nếu chuyển sang private mà allowedMembers chưa init
        if (Boolean.TRUE.equals(existing.getIsPrivate()) && existing.getAllowedMembers() == null) {
            existing.setAllowedMembers(new HashSet<>());
        }

        return channelRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteChannel(Long id, String username) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found with id " + id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        checkManageChannelPermission(channel.getServer().getId(), user.getId());

        channelRepository.deleteById(id);
    }

    private void checkManageChannelPermission(Long serverId, Long userId) {
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên server này"));

        boolean isOwner = member.getServer().getOwner().getId().equals(userId);

        boolean hasPermission = member.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getCode().equals(EPermission.ADMIN.getCode())
                        || p.getCode().equals(EPermission.MANAGE_CHANNELS.getCode()));

        if (!isOwner && !hasPermission) {
            throw new AccessDeniedException("Bạn không có quyền Quản lý kênh (Sửa/Xóa).");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Channel> getChannelById(Long id) {
        return channelRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Channel> getAllChannels() {
        return channelRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Channel> getChannelsByServer(Long serverId){
        return channelRepository.findByServer_Id(serverId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Channel> getChannelsByCategory(Long categoryId){
        return channelRepository.findByCategory_Id(categoryId);
    }

    // Hiển thị theo quyền xem
    @Override
    @Transactional(readOnly = true)
    public List<Channel> getChannelsByServerVisibleToUser(Long serverId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên server này"));

        boolean canViewAll = member.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getCode().equals(EPermission.ADMIN.getCode())
                        || p.getCode().equals(EPermission.MANAGE_CHANNELS.getCode()))
                || member.getServer().getOwner().getId().equals(user.getId());

        List<Channel> channels = channelRepository.findByServer_Id(serverId);
        if (canViewAll) return channels;

        return channels.stream()
                .filter(c -> Boolean.FALSE.equals(c.getIsPrivate())
                        || (c.getAllowedMembers() != null && c.getAllowedMembers().contains(member)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Channel> getChannelsByCategoryVisibleToUser(Long categoryId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<Channel> channels = channelRepository.findByCategory_Id(categoryId);
        if (channels.isEmpty()) return channels;

        Long serverId = channels.get(0).getServer().getId();
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên server này"));

        boolean canViewAll = member.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getCode().equals(EPermission.ADMIN.getCode())
                        || p.getCode().equals(EPermission.MANAGE_CHANNELS.getCode()))
                || member.getServer().getOwner().getId().equals(user.getId());

        if (canViewAll) return channels;

        return channels.stream()
                .filter(c -> Boolean.FALSE.equals(c.getIsPrivate())
                        || (c.getAllowedMembers() != null && c.getAllowedMembers().contains(member)))
                .collect(Collectors.toList());
    }
}