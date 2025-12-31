package hcmute.edu.vn.discord.entity.mongo;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Document(collection = "direct_messages")
@Data
@Builder
public class DirectMessage {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    private Long senderId;
    private Long receiverId;

    private String content;

    private boolean deleted;
    private boolean edited;

    private Map<Long, String> reactions;

    private Date createdAt;
    private Date updatedAt;
}
