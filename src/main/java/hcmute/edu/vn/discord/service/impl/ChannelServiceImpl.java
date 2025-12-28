package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.service.ChannelService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;

    @Override
    @Transactional
    public Channel createChannel(Channel channel) {
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
