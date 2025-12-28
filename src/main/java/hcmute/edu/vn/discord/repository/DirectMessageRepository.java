package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.mongo.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends MongoRepository<DirectMessage, String> {

    // Lấy tin nhắn theo conversationId
    List<DirectMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    Page<DirectMessage> findByConversationId(String conversationId, Pageable pageable);
}
