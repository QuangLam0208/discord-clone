package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Permission;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;

import java.util.List;
import java.util.Set;

public interface ServerMemberService {
    ServerMember addMemberToServer(Long serverId, Long userId);
    List<ServerMember> getMembersByServerId(Long serverId);
    boolean isMember(Long serverId, Long userId);
    boolean removeMember(Long serverId, Long userId);
    boolean userHasAnyPermission(Long serverId, Long userId, Set<String> requiredCodes);
    List<ServerRole> getRolesOfMember(Long serverId, Long userId);
    Set<Permission> getEffectivePermissions(Long serverId, Long userId);
}