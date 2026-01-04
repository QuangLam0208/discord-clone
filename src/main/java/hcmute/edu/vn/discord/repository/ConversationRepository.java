package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.mongo.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    List<Conversation> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);

    Optional<Conversation> findByUser1IdAndUser2IdOrUser1IdAndUser2Id(Long u1a, Long u2a, Long u1b, Long u2b);
}
