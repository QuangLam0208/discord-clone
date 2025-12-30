package hcmute.edu.vn.discord.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ServerDetailResponse {
    private ServerResponse serverInfo;
    private List<CategoryResponse> categories;
    private List<ChannelResponse> channels; // Những kênh không thuộc category hoặc list tổng
    private List<ServerMemberResponse> members;
}