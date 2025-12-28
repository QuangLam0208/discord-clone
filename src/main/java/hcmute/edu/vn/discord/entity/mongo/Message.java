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

    @Data
    public static class Reaction {
        private Long userId;
        private String emoji;
    }
}