package hcmute.edu.vn.discord.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {
    private Long friendUserId;
    private String friendUsername;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime since;
}
