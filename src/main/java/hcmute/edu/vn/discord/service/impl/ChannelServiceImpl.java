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
    // Inject thêm 2 repo này để check quyền
    private final UserRepository userRepository;
    private final ServerMemberRepository serverMemberRepository;

    @Override
    @Transactional
    public Channel createChannel(Channel channel, String creatorUsername) {
        // 1. Tìm người tạo
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + creatorUsername));

        // Kênh bắt buộc phải thuộc về 1 Server (trừ khi là DM, nhưng DM thường dùng service riêng)
        if (channel.getServer() == null) {
            throw new IllegalArgumentException("Channel must belong to a server");
        }

        Long serverId = channel.getServer().getId();

        // 2. Tìm Member trong Server
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
    public Channel updateChannel(Long id, Channel updatedChannel) {
        Channel existing = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found with id " + id));

        existing.setName(updatedChannel.getName());
        existing.setIsPrivate(updatedChannel.getIsPrivate());
        existing.setCategory(updatedChannel.getCategory());
        // Nếu cập nhật permission (private/public) thì cần xử lý thêm ở đây tuỳ nghiệp vụ

        return channelRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteChannel(Long id) {
        if (!channelRepository.existsById(id)) {
            throw new EntityNotFoundException("Channel not found with id " + id);
        }
        channelRepository.deleteById(id);
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