package hcmute.edu.vn.discord.dto.request;

import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateServerStatusRequest {
    @NotNull
    private ServerStatus status; // ACTIVE, FREEZE, DELETED
}