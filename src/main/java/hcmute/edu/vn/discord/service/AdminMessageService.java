package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.AdminMessageSearchRequest;
import hcmute.edu.vn.discord.dto.response.AdminMessagePageResponse;

public interface AdminMessageService {
    AdminMessagePageResponse search(AdminMessageSearchRequest req);
    void softDelete(String messageId, String adminUsername);
    void restore(String messageId, String adminUsername);
}