package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.AssignRolesRequest;
import hcmute.edu.vn.discord.dto.request.ServerRoleRequest;
import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import hcmute.edu.vn.discord.entity.jpa.ServerRole;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ServerRoleService {
    @Transactional
    ServerRole createRole(Long serverId, ServerRoleRequest request, String actorUsername);

    @Transactional
    ServerRole updateRole(Long serverId, Long roleId, ServerRoleRequest request, String actorUsername);

    @Transactional
    void deleteRole(Long serverId, Long roleId, String actorUsername);

    @Transactional(readOnly = true)
    List<ServerRole> listRoles(Long serverId, String actorUsername);

    @Transactional
    ServerMember assignRoles(Long serverId, AssignRolesRequest request, String actorUsername);

    @Transactional(readOnly = true)
    List<ServerMember> getMembersByRole(Long serverId, Long roleId, String actorUsername);
}
