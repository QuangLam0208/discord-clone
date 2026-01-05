package hcmute.edu.vn.discord.dto.request;

import lombok.Data;

@Data
public class TransferOwnerRequest {
    // Bắt buộc truyền memberId của người sẽ trở thành chủ mới
    private Long newOwnerMemberId;
}