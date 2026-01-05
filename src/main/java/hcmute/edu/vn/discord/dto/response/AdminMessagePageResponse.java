package hcmute.edu.vn.discord.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminMessagePageResponse {
    private List<AdminMessageItemResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}