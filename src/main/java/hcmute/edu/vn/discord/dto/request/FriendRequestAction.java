package hcmute.edu.vn.discord.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendRequestAction {
    @NotNull(message = "TargetUserId không được trống")
    private Long targetUserId;
}
