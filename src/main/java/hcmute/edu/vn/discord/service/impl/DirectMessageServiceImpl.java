package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.mongo.DirectMessage;
import hcmute.edu.vn.discord.repository.DirectMessageRepository;
import hcmute.edu.vn.discord.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectMessageServiceImpl implements DirectMessageService {

    private final DirectMessageRepository directMessageRepository;

    @Override
    public DirectMessage sendMessage(Long senderId, Long receiverId, String content) {

        DirectMessage message = new DirectMessage();
        message.setParticipants(Arrays.asList(senderId, receiverId));
        message.setSenderId(senderId);
        message.setContent(content);
        message.setCreatedAt(new Date());

        return directMessageRepository.save(message);
    }

    @Override
    public List<DirectMessage> getConversation(Long userId1, Long userId2) {
        return directMessageRepository.findConversation(
                userId1,
                userId2,
                Sort.by(Sort.Direction.ASC, "createdAt")
        );
    }

    @Override
    public List<Long> getChatPartners(Long userId) {

        return directMessageRepository
                .findByParticipantsContaining(userId)
                .stream()
                .flatMap(dm -> dm.getParticipants().stream())
                .filter(id -> !id.equals(userId))
                .distinct()
                .collect(Collectors.toList());
    }
}
