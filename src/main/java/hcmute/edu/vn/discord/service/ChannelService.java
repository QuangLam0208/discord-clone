package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Channel;

import java.util.List;
import java.util.Optional;

public interface ChannelService {
    Channel createChannel(Channel channel);
    Channel updateChannel(Long id, Channel channel);
    void deleteChannel(Long id);
    Optional<Channel> getChannelById(Long id);
    List<Channel> getAllChannels();
    List<Channel> getChannelsByServer(Long serverId);
    List<Channel> getChannelsByCategory(Long categoryId);
}
