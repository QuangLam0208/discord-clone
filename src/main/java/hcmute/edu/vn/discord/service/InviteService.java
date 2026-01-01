package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Invite;
import java.util.List;
import java.util.Optional;

public interface InviteService {
    // Tạo lời mời mới (hết hạn sau expireSeconds giây, 0 = vĩnh viễn)
    Invite createInvite(Long serverId, Integer maxUses, Long expireSeconds);

    // Lấy thông tin invite theo code
    Optional<Invite> getInviteByCode(String code);

    // Xử lý khi user click vào invite (tăng lượt dùng)
    boolean useInvite(String code);

    // XỬ LÝ GIA NHẬP SERVER
    void joinServer(String code);

    // Lấy danh sách invite của server
    List<Invite> getInvitesByServer(Long serverId);

    // Xóa/Vô hiệu hóa invite
    void revokeInvite(Long inviteId);
}