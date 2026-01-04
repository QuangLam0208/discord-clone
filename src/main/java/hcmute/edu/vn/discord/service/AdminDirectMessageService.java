package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.AdminDirectMessageSearchRequest;
import hcmute.edu.vn.discord.dto.response.AdminMessagePageResponse;
import org.springframework.transaction.annotation.Transactional;

public interface AdminDirectMessageService {
    @Transactional(readOnly = true)
    AdminMessagePageResponse search(AdminDirectMessageSearchRequest req);

    @Transactional
    void softDelete(String messageId, String adminUsername);

    @Transactional
    void restore(String messageId, String adminUsername);
}
