package hcmute.edu.vn.discord.entity.mongo;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "direct_messages")
@Data
public class DirectMessage {

    @Id
    private String id;

    @Indexed
    private List<Long> participants; // [userAId, userBId]

    private Long senderId;
    private String content;

    private Date createdAt = new Date();
}

