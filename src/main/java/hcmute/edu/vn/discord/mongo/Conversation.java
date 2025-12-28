package hcmute.edu.vn.discord.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    private String id;

    private Long user1Id;
    private Long user2Id;

    private Date createdAt;
    private Date updatedAt;
}
