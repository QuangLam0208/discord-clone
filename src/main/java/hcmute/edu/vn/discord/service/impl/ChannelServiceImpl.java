package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.dto.response.ChannelResponse;
import hcmute.edu.vn.discord.entity.jpa.Category;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.service.ChannelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;
    private final CategoryRepository categoryRepository;

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
        // Lưu ý: Thường ít khi cho đổi Type (Text <-> Voice) sau khi tạo, tùy ông quyết định
        // channel.setType(request.getType());

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
        } else {
            // Nếu request gửi null categoryId -> Có thể user muốn channel "rời khỏi" category (nằm ở ngoài)
            // Hoặc nếu ông muốn "null nghĩa là không update" thì check null kỹ hơn.
            // Ở đây tôi giả định null là set về null (nằm ngoài category)
            channel.setCategory(null);
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
        return channelRepository.findByServerId(serverId).stream()
                .map(ChannelResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponse> getChannelsByCategory(Long categoryId) {
        return channelRepository.findByCategoryId(categoryId).stream()
                .map(ChannelResponse::from)
                .collect(Collectors.toList());
    }
}