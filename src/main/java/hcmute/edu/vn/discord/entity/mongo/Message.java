package hcmute.edu.vn.discord.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "messages")
@Data
public class Message {
    @Id
    private String id;

    @Indexed
    private Long channelId;
    private Long senderId;

    private String content;
    private Boolean deleted = false;
    private Boolean isEdited = false; // Đánh dấu đã sửa
    private Date createdAt = new Date();

    // Tính năng mới
    private String replyToId;
    private List<String> attachments = new ArrayList<>();

    private List<Reaction> reactions = new ArrayList<>();

    /**
     * Returns a defensive copy of the reactions list to avoid exposing
     * the internal mutable representation.
     */
    public List<Reaction> getReactions() {
        return new ArrayList<>(reactions);
    }

    /**
     * Sets the reactions list using a defensive copy to avoid retaining
     * references to mutable lists supplied by callers.
     */
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