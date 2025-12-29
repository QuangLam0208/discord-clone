package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Channel;

import java.util.List;
import java.util.Optional;

public interface ChannelService {
    Channel createChannel(Channel channel, String creatorUsername);
    Channel updateChannel(Long id, Channel updatedChannel, String username);
    void deleteChannel(Long id, String username);
    Optional<Channel> getChannelById(Long id);
    List<Channel> getAllChannels();
    List<Channel> getChannelsByServer(Long serverId);
    List<Channel> getChannelsByCategory(Long categoryId);
}
