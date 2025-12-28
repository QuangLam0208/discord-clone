package hcmute.edu.vn.discord.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Builder
@Data
public class DirectMessageResponse {
    private String id;
    private String conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Boolean recalled;
    private Date createdAt;
    private boolean edited;
    private boolean deleted;

    private Map<Long, String> reactions;

    private Date updatedAt;
}
