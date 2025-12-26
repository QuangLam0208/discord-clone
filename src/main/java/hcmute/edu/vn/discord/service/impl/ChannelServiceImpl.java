package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.service.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChannelServiceImpl implements ChannelService {

    @Autowired
    private ChannelRepository channelRepository;

    @Override
    public Channel createChannel(Channel channel) {
        return channelRepository.save(channel);
    }

    @Override
    public Channel updateChannel(Long id, Channel updatedChannel) {
        Channel existing = channelRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Channel not found with id " + id));

        existing.setName(updatedChannel.getName());
        existing.setIsPrivate(updatedChannel.getIsPrivate());
        existing.setCategory(updatedChannel.getCategory());
        existing.setServer(updatedChannel.getServer());
        existing.setType(updatedChannel.getType());

        return channelRepository.save(existing);
    }

    @Override
    public void deleteChannel(Long id) {
        if(!channelRepository.existsById(id)) {
            throw new RuntimeException("Channel not found with id " + id);
        }

        channelRepository.deleteById(id);
    }

    @Override
    public Optional<Channel> getChannelById(Long id) {
        return channelRepository.findById(id);
    }

    @Override
    public List<Channel> getAllChannels() {
        return channelRepository.findAll();
    }

    @Override
    public List<Channel> getChannelsByServer(Long serverId){
        return channelRepository.findByServer_Id(serverId);
    }

    @Override
    public List<Channel> getChannelsByCategory(Long categoryId){
        return channelRepository.findByCategory_Id(categoryId);
    }
}
