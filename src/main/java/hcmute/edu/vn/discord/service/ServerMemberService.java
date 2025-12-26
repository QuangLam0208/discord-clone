package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.ServerMember;
import java.util.List;

public interface ServerMemberService {
    // Thêm thành viên vào server
    ServerMember addMemberToServer(Long serverId, Long userId);

    // Lấy danh sách thành viên của server
    List<ServerMember> getMembersByServerId(Long serverId);

    // Kiểm tra xem user có phải là thành viên của server không
    boolean isMember(Long serverId, Long userId);

    // Xóa thành viên khỏi server (Rời nhóm hoặc bị kick)
    void removeMember(Long serverId, Long userId);
}