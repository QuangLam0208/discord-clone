package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.RegisterRequest;
import hcmute.edu.vn.discord.dto.response.ServerSummaryResponse;
import hcmute.edu.vn.discord.dto.response.UserDetailResponse;
import hcmute.edu.vn.discord.entity.enums.ERole;
import hcmute.edu.vn.discord.entity.jpa.Role;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.repository.ServerMemberRepository;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final MessageRepository messageRepository;

    @Override
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Role defaultRole = roleRepository.findByName(ERole.USER_DEFAULT)
                .orElseThrow(() -> new EntityNotFoundException("Role user not found"));

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(defaultRole);

        return userRepository.save(user);
    }

    @Override
    public void registerUser(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername()))
            throw new IllegalArgumentException("Username already exists");

        if (userRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email already exists");

        Role defaultRole = roleRepository.findByName(ERole.USER_DEFAULT)
                .orElseThrow(() -> new EntityNotFoundException("Role user not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword().trim()))
                .displayName(request.getDisplayName())
                .isActive(true)
                .isEmailVerified(false)
                .roles(roles)
                .build();

        userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserDetailResponse getUserDetail(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        List<Server> servers = serverRepository.findDistinctByMembers_User_Id(u.getId());
        List<ServerSummaryResponse> summaries = servers.stream()
                .map(s -> ServerSummaryResponse.from(
                        s,
                        channelRepository.countByServerId(s.getId()),
                        serverMemberRepository.countByServerId(s.getId())
                ))
                .toList();

        // Lấy message mới nhất => Instant
        Instant lastActiveInstant = messageRepository.findFirstBySenderIdOrderByCreatedAtDesc(u.getId())
                .map(m -> m.getCreatedAt().toInstant())
                .orElse(null);

        // Chọn timezone chuyển đổi (UTC hoặc systemDefault)
        ZoneId zone = ZoneId.systemDefault(); // hoặc: ZoneOffset.UTC

        // Nếu DTO của bạn mong muốn LocalDateTime:
        LocalDateTime lastActiveLdt = (lastActiveInstant != null)
                ? LocalDateTime.ofInstant(lastActiveInstant, zone)
                : null;

        // Nếu u.getCreatedAt là java.util.Date, chuyển sang LocalDateTime cho đồng nhất
        Object createdAtObj = u.getCreatedAt();
        LocalDateTime createdAtLdt = null;
        if (createdAtObj instanceof Instant) {
            createdAtLdt = LocalDateTime.ofInstant((Instant) createdAtObj, zone);
        } else if (createdAtObj instanceof java.util.Date) {
            createdAtLdt = LocalDateTime.ofInstant(((java.util.Date) createdAtObj).toInstant(), zone);
        } else if (createdAtObj instanceof LocalDateTime) {
            createdAtLdt = (LocalDateTime) createdAtObj;
        }

        return UserDetailResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .displayName(u.getDisplayName())
                .birthDate(u.getBirthDate())
                .createdAt(createdAtLdt)     // nếu DTO là LocalDateTime; nếu DTO là Instant thì set trực tiếp Instant
                .lastActive(lastActiveLdt)   // chuyển đổi đúng cách; có thể null
                .servers(summaries)
                .build();
    }
}