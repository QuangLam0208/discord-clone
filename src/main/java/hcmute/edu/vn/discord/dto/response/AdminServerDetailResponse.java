package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminServerDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private ServerStatus status;

    private Long ownerId;
    private String ownerUsername;
    private String ownerDisplayName;

    private int memberCount;
    private int channelCount;
    private int roleCount;

    private List<String> channelNames;
    private List<String> roleNames;
    private List<String> memberNames; // Limit to e.g. 50
}
