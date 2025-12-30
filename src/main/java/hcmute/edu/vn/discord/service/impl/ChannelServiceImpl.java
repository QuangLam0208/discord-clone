package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.ChannelPermissionRequest;
import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.dto.response.ChannelResponse;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.security.servers.ServerAuth;
import hcmute.edu.vn.discord.service.ChannelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;
    private final CategoryRepository categoryRepository;
    private final ServerAuth serverAuth;
    private final ServerMemberRepository serverMemberRepository;
    private final ServerRoleRepository serverRoleRepository;

    @Override
    @Transactional
    public ChannelResponse createChannel(ChannelRequest request) {
        // 1. Tìm Server (Bắt buộc)
        Server server = serverRepository.findById(request.getServerId())
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));

        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setType(request.getType()); // TEXT hoặc VOICE
        channel.setServer(server);
        channel.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false);

        // 2. Xử lý Category (Nếu có)
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found"));

            // Logic an toàn: Đảm bảo Category thuộc về đúng Server này
            if (!category.getServer().getId().equals(server.getId())) {
                throw new IllegalArgumentException("Category does not belong to this Server");
            }
            channel.setCategory(category);
        }

        Channel savedChannel = channelRepository.save(channel);
        return ChannelResponse.from(savedChannel);
    }

    @Override
    @Transactional
    public ChannelResponse updateChannel(Long channelId, ChannelRequest request) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));

        // Update các trường cơ bản
        channel.setName(request.getName());
        if (request.getIsPrivate() != null) {
            channel.setIsPrivate(request.getIsPrivate());
        }

        // Update Category (Di chuyển channel sang category khác)
        if (request.getCategoryId() != null) {
            // Nếu gửi ID mới khác ID cũ
            if (channel.getCategory() == null || !request.getCategoryId().equals(channel.getCategory().getId())) {
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new EntityNotFoundException("Category not found"));

                // Check server khớp
                if (!category.getServer().getId().equals(channel.getServer().getId())) {
                    throw new IllegalArgumentException("Category does not belong to this Server");
                }
                channel.setCategory(category);
            }
        }

        return ChannelResponse.from(channelRepository.save(channel));
    }

    @Override
    @Transactional
    public void deleteChannel(Long channelId) {
        if (!channelRepository.existsById(channelId)) {
            throw new EntityNotFoundException("Channel not found");
        }
        channelRepository.deleteById(channelId);
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelResponse getChannelById(Long id) {
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));
        return ChannelResponse.from(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponse> getChannelsByServer(Long serverId) {
        // Lấy username hiện tại
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return channelRepository.findByServerId(serverId).stream()
                // FIX: Lọc bỏ các kênh mà user không có quyền xem
                .filter(channel -> serverAuth.canViewChannel(channel.getId(), username))
                .map(ChannelResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponse> getChannelsByCategory(Long categoryId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return channelRepository.findByCategoryId(categoryId).stream()
                // FIX: Cũng áp dụng lọc ở đây
                .filter(channel -> serverAuth.canViewChannel(channel.getId(), username))
                .map(ChannelResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChannelResponse updateChannelPermissions(Long channelId, ChannelPermissionRequest request) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));

        if (!Boolean.TRUE.equals(channel.getIsPrivate())) {
            throw new IllegalArgumentException("Chỉ kênh Private mới được thiết lập quyền truy cập");
        }

        Long serverId = channel.getServer().getId();

        // 1. Xử lý Members
        if (request.getMemberIds() != null) {
            List<ServerMember> members = serverMemberRepository.findAllById(request.getMemberIds());

            // Validate: Member phải thuộc server
            Set<ServerMember> validMembers = new HashSet<>();
            for (ServerMember m : members) {
                if (m.getServer().getId().equals(serverId)) {
                    validMembers.add(m);
                }
            }
            channel.setAllowedMembers(validMembers);
        }

        // 2. Xử lý Roles
        if (request.getRoleIds() != null) {
            List<ServerRole> roles = serverRoleRepository.findAllById(request.getRoleIds());

            // Validate: Role phải thuộc server
            Set<ServerRole> validRoles = new HashSet<>();
            for (ServerRole r : roles) {
                if (r.getServer().getId().equals(serverId)) {
                    validRoles.add(r);
                }
            }
            channel.setAllowedRoles(validRoles);
        }

        return ChannelResponse.from(channelRepository.save(channel));
    }
}