package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import java.util.List;

public interface ServerMemberService {
    ServerMember addMemberToServer(Long serverId, Long userId);
    List<ServerMember> getMembersByServerId(Long serverId);
    boolean isMember(Long serverId, Long userId);
    void removeMember(Long serverId, Long userId);
}