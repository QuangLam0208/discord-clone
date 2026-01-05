package hcmute.edu.vn.discord.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteResponse {
    private String code;
    private Long serverId;
    private String serverName;
    private String serverIconUrl;
    private Integer maxUses;
    private Integer usedCount;
    private Date expiresAt;
}
