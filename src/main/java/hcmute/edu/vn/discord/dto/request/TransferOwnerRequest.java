package hcmute.edu.vn.discord.dto.request;

import lombok.Data;

@Data
public class TransferOwnerRequest {
    private Long newOwnerId;
    private String newOwnerUsername;
}
