package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
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

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final ServerRepository serverRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Channel createChannel(Long serverId, String name, ChannelType type, Long categoryId, Boolean isPrivate) {
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

        return channelRepository.save(channel);
    }

    @Override
    @Transactional
    public Channel updateChannel(Long channelId, String name, Long categoryId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found"));

        if (name != null && !name.isBlank()) channel.setName(name);

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
        return channelRepository.save(channel);
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