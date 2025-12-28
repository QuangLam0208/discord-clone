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
    /**
     * Indicates that the sender has recalled (retracted) this message.
     * <p>
     * A recalled message typically remains in the system but is presented to
     * participants as \"recalled\" or with its content hidden, according to
     * client-side rules.
     */
    private Boolean recalled;
    private Date createdAt;
    private boolean edited;
    /**
     * Indicates that this message has been deleted at the conversation/system level.
     * <p>
     * This flag is distinct from {@link #recalled}: it is intended for cases such as
     * soft-deletion, moderation removal, or removal from history, even if the
     * sender did not explicitly recall the message.
     */
    private boolean deleted;

    private Map<Long, String> reactions;

    private Date updatedAt;
}
