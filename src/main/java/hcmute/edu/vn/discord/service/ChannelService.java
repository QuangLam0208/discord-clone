package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.ChannelRequest;
import hcmute.edu.vn.discord.dto.response.ChannelResponse;
import java.util.List;

public interface ChannelService {
    ChannelResponse createChannel(ChannelRequest request);
    ChannelResponse updateChannel(Long channelId, ChannelRequest request);
    void deleteChannel(Long channelId);

    ChannelResponse getChannelById(Long id);
    List<ChannelResponse> getChannelsByServer(Long serverId);
    List<ChannelResponse> getChannelsByCategory(Long categoryId);
}