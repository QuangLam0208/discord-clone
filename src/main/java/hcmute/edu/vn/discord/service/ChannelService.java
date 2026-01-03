package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.jpa.Channel;

import java.util.List;

public interface ChannelService {
    Channel createChannel(Long serverId, String name, ChannelType type, Long categoryId, Boolean isPrivate, String createdByUsername);
    Channel updateChannel(Long channelId, String name, Long categoryId);
    void deleteChannel(Long channelId);
    List<Channel> getChannelsByServer(Long serverId);
    Channel getChannelById(Long channelId);
}