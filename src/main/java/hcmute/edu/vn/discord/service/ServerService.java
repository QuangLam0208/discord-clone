package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.request.TransferOwnerRequest;
import hcmute.edu.vn.discord.dto.response.ServerMemberSummaryResponse;
import hcmute.edu.vn.discord.dto.response.ServerResponse;
import hcmute.edu.vn.discord.entity.jpa.AuditLog;
import hcmute.edu.vn.discord.entity.jpa.Server;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ServerService {
    Server createServer(ServerRequest request);

    Server updateServer(Long id, ServerRequest request);

    Server getServerById(Long id);

    List<Server> getAllServers();

    List<ServerResponse> getMyServersResponses();

    List<Server> getServersByCurrentUsername();

    void deleteServer(Long serverId);

    int countOnlineMembers(Long serverId);

    List<AuditLog> getAuditLogs(Long serverId);

    List<ServerMemberSummaryResponse> getMembersOfServer(Long serverId);

    @Transactional
    void transferOwner(Long serverId, TransferOwnerRequest req);
}