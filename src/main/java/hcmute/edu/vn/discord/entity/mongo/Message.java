package hcmute.edu.vn.discord.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "messages")
@CompoundIndex(name = "sender_created_idx", def = "{'senderId': 1, 'createdAt': -1}")
@Data
public class Message {
    @Id
    private String id;

    @Indexed
    private Long channelId;
    private Long senderId;

    private String content;
    private Boolean deleted = false;
    private Boolean isEdited = false;
    private Date createdAt = new Date();

    private String replyToId;
    private List<String> attachments = new ArrayList<>();
    private List<Reaction> reactions = new ArrayList<>();

    public List<Reaction> getReactions() {
        return new ArrayList<>(reactions);
    }
    public void setReactions(List<Reaction> reactions) {
        if (reactions == null) {
            this.reactions = new ArrayList<>();
        } else {
            this.reactions = new ArrayList<>(reactions);
        }
    }

    @Data
    public static class Reaction {
        private Long userId;
        private String emoji;
    }
}