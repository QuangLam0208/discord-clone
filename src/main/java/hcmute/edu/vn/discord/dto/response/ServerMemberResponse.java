package hcmute.edu.vn.discord.dto.response;

import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class ServerMemberResponse {
    private Long id;
    private ServerMemberSummary user;
    private String nickname;
    private LocalDateTime joinedAt;
    private Boolean isBanned;
    private Set<String> roles;

    public static ServerMemberResponse from(ServerMember m) {
        if (m == null) return null;
        return ServerMemberResponse.builder()
                .id(m.getId())
                .user(ServerMemberSummary.from(m.getUser()))
                .nickname(m.getNickname())
                .joinedAt(m.getJoinedAt())
                .isBanned(m.getIsBanned())
                .roles(m.getRoles() == null ? Set.of() :
                        m.getRoles().stream()
                                .map(ServerRole::getName)
                                .collect(Collectors.toSet()))
                .build();
    }
}