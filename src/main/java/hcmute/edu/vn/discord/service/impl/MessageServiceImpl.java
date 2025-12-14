package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.mongo.Message;
import hcmute.edu.vn.discord.repository.MessageRepository;
import hcmute.edu.vn.discord.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class MessageServiceImpl implements MessageService {
    @Autowired
    private MessageRepository messageRepository;

    @Override
    public Message sendMessage(Message message) {
        message.setCreatedAt(new Date());
        message.setDeleted(false);
        return messageRepository.save(message);
    }

    @Override
    public Optional<Message> getMessageById(String id) {
        return messageRepository.findById(id);
    }

    @Override
    public List<Message> getMessagesByChannel(Long channelId) {
        return messageRepository.findByChannelId(channelId);
    }

    @Override
    public Message editMessage(String id, String newContent) {
        Message existing = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found with id " + id));

        existing.setContent(newContent);
        return messageRepository.save(existing);
    }

    @Override
    public void deleteMessage(String id) {
        Message existing = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found with id " + id));
        existing.setDeleted(true);
        messageRepository.save(existing);
    }

    @Override
    public Message addReaction(String messageId, Long userId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id " + messageId));

        Message.Reaction reaction = new Message.Reaction();
        reaction.setUserId(userId);
        reaction.setEmoji(emoji);
        message.getReactions().add(reaction);

        return messageRepository.save(message);
    }

    @Override
    public Message removeReaction(String messageId, Long userId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id " + messageId));

        message.getReactions().removeIf(
                r -> r.getUserId().equals(userId) && r.getEmoji().equals(emoji)
        );
        return messageRepository.save(message);
    }
}
