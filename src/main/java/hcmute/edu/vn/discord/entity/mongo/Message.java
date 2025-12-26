package hcmute.edu.vn.discord.entity.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.Data;
import java.util.List;
import java.util.Date;

@Document(collection = "messages")
@Data
public class Message {
    @Id
    private String id; // MongoDB ID là String (ObjectId)

    @Indexed // Tạo index để query nhanh
    private Long channelId;

    private Long senderId;
    private String content;
    private Boolean deleted = false;
    private Date createdAt = new Date();

    // Nhúng (Embed) reactions trực tiếp vào Message để không phải join bảng
    private List<Reaction> reactions;

    @Data
    public static class Reaction {
        private Long userId;
        private String emoji;
    }
}

