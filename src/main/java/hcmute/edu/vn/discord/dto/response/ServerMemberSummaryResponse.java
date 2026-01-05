package hcmute.edu.vn.discord.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerMemberSummaryResponse {
    private Long memberId;
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private boolean isOwner;
    private boolean isAdminRole;
}