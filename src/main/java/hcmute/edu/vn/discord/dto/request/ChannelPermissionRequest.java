package hcmute.edu.vn.discord.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class ChannelPermissionRequest {
    // Danh sách ID của các thành viên được phép xem (ServerMember ID)
    private List<Long> memberIds;

    // Danh sách ID của các Role được phép xem (ServerRole ID)
    private List<Long> roleIds;
}