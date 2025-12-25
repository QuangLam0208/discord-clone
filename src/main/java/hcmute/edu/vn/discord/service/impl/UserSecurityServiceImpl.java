package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.entity.jpa.UserSecurity;
import hcmute.edu.vn.discord.repository.UserSecurityRepository;
import hcmute.edu.vn.discord.service.UserSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSecurityServiceImpl implements UserSecurityService {
    private final UserSecurityRepository userSecurityRepository;

    @Override
    public UserSecurity getByUserId(Long userId) {
        return userSecurityRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("UserSecurity not found"));
    }

    @Override
    @Transactional
    public UserSecurity createDefault(User user) {

        UserSecurity security = new UserSecurity();
        security.setUser(user);
        security.setTwoFaEnabled(false);
        security.setTwoFaSecret(null);

        return userSecurityRepository.save(security);
    }

    @Override
    @Transactional
    public void enableTwoFa(Long userId, String secret) {
        UserSecurity security = getByUserId(userId);
        security.setTwoFaEnabled(true);
        security.setTwoFaSecret(secret);
    }

    @Override
    @Transactional
    public void disableTwoFa(Long userId) {
        UserSecurity security = getByUserId(userId);
        security.setTwoFaEnabled(false);
        security.setTwoFaSecret(null);
    }

    @Override
    public boolean isTwoFaEnabled(Long userId) {
        return getByUserId(userId).getTwoFaEnabled();
    }
}
