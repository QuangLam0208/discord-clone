package hcmute.edu.vn.discord.dto.response;

import lombok.*;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationResponse {

    private String id;
    private Long user1Id;
    private Long user2Id;
    private Date updatedAt;
}
