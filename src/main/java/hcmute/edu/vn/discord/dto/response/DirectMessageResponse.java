package hcmute.edu.vn.discord.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private List<String> attachments;
    private String replyToId;
    private DirectMessageResponse replyToMessage;
    private boolean isRead;
    private Map<String, Set<Long>> reactions;

    private Date updatedAt;
}
