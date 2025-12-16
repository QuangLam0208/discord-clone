package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.mongo.DirectMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends MongoRepository<DirectMessage, String> {
    // Lấy lịch sử chat giữa 2 người
    // participants là List<Long> [user1, user2] -> Cần query chứa cả 2 ID này
    @Query("{ 'participants': { $all: [?0, ?1] } }")
    List<DirectMessage> findConversation(Long userId1, Long userId2);

    // Lấy danh sách các cuộc trò chuyện của 1 user (để hiển thị list bên trái)
    List<DirectMessage> findByParticipantsContaining(Long userId);
}
