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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final ServerMemberRepository serverMemberRepository;

    @Override
    @Transactional
    public Channel createChannel(Channel channel, String creatorUsername) {
        // 1. Tìm người tạo
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + creatorUsername));

        if (channel.getServer() == null) {
            throw new IllegalArgumentException("Channel must belong to a server");
        }

        Long serverId = channel.getServer().getId();

        // 2. Kiểm tra người tạo có phải thành viên Server không
        ServerMember member = serverMemberRepository.findByServerIdAndUserId(serverId, creator.getId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không phải là thành viên của Server này"));

        // 3. CHECK QUYỀN: Phải là Owner hoặc có quyền MANAGE_CHANNELS / ADMIN
        boolean isOwner = channel.getServer().getOwner().getId().equals(creator.getId());

        boolean hasPermission = member.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getCode().equals(EPermission.ADMIN.getCode())
                        || p.getCode().equals(EPermission.MANAGE_CHANNELS.getCode()));

        if (!isOwner && !hasPermission) {
            throw new AccessDeniedException("Bạn không có quyền tạo kênh (Cần quyền MANAGE_CHANNELS)");
        }

        // 4. LOGIC KÊNH PRIVATE: Tự động add người tạo vào danh sách được xem
        if (Boolean.TRUE.equals(channel.getIsPrivate())) {
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

        // CHECK QUYỀN SỬA KÊNH
        checkManageChannelPermission(existing.getServer().getId(), user.getId());

        existing.setName(updatedChannel.getName());
        existing.setIsPrivate(updatedChannel.getIsPrivate());
        existing.setCategory(updatedChannel.getCategory());
        // Nếu bạn muốn update cả allowedRoles/Members thì map thêm ở đây

        return channelRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteChannel(Long id, String username) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found with id " + id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // CHECK QUYỀN XÓA KÊNH
        checkManageChannelPermission(channel.getServer().getId(), user.getId());

        channelRepository.deleteById(id);
    }

    // --- HELPER CHECK QUYỀN QUẢN LÝ (Sửa/Xóa) ---
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
}