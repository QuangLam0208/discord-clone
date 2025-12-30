package hcmute.edu.vn.discord.config;

import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.ERole;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Chỉ chạy khi chưa có dữ liệu user
        if (userRepository.count() > 0) {
            return;
        }

        System.out.println(">>> ĐANG KHỞI TẠO DỮ LIỆU MẪU (SEEDING)...");

        // 0. Tạo Permissions
        createPermissionsIfNotExist();

        // 1. Tạo Roles
        createRolesIfNotExist();

        // 2. Tạo 10 Users (1 Admin + 9 User)
        List<User> users = createUsers();

        // 3. Tạo 4 Server với số lượng thành viên khác nhau
        // Server 1: 1 member (Chỉ có Owner)
        createServerWithMembers(users.get(0), "Admin's Server", "Server riêng tư",
                List.of()); // Không thêm ai

        // Server 2: 2 members (Owner + 1 user)
        createServerWithMembers(users.get(1), "Duo Gaming", "Chơi game cặp",
                List.of(users.get(2)));

        // Server 3: 3 members (Owner + 2 users)
        createServerWithMembers(users.get(3), "Trio Coding", "Hội lập trình 3 người",
                List.of(users.get(4), users.get(5)));

        // Server 4: 4 members (Owner + 3 users)
        createServerWithMembers(users.get(6), "Squad Community", "Cộng đồng lớn",
                List.of(users.get(7), users.get(8), users.get(9)));

        System.out.println(">>> KHỞI TẠO DỮ LIỆU THÀNH CÔNG! <<<");
    }

    private void createPermissionsIfNotExist() {
        for (hcmute.edu.vn.discord.entity.enums.EPermission ePerm : hcmute.edu.vn.discord.entity.enums.EPermission
                .values()) {
            if (!permissionRepository.existsByCode(ePerm.getCode())) {
                Permission p = new Permission();
                p.setCode(ePerm.getCode());
                p.setDescription(ePerm.getDescription());
                permissionRepository.save(p);
            }
        }
    }

    private void createRolesIfNotExist() {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(null, ERole.ADMIN, null));
            roleRepository.save(new Role(null, ERole.USER_DEFAULT, null));
            roleRepository.save(new Role(null, ERole.USER_PREMIUM, null));
        }

        // Tự động gán quyền ADD_REACTIONS cho USER_DEFAULT nếu chưa có (để fix lỗi user
        // vừa gặp)
        Role defaultRole = roleRepository.findByName(ERole.USER_DEFAULT).orElse(null);
        if (defaultRole != null) {
            Permission addReaction = permissionRepository
                    .findByCode(hcmute.edu.vn.discord.entity.enums.EPermission.ADD_REACTIONS.getCode()).orElse(null);
            if (addReaction != null) {
                if (defaultRole.getPermissions() == null) {
                    defaultRole.setPermissions(new HashSet<>());
                }
                if (!defaultRole.getPermissions().contains(addReaction)) {
                    defaultRole.getPermissions().add(addReaction);
                    roleRepository.save(defaultRole);
                }
            }
        }
    }

    private List<User> createUsers() {
        List<User> createdUsers = new ArrayList<>();
        Role adminRole = roleRepository.findByName(ERole.ADMIN).orElseThrow();
        Role userRole = roleRepository.findByName(ERole.USER_DEFAULT).orElseThrow();

        // Tạo Admin
        createdUsers.add(saveUser("admin", "admin@gmail.com", "System Admin", Set.of(adminRole, userRole)));

        // Tạo 9 Users thường (user1 -> user9)
        for (int i = 1; i <= 9; i++) {
            createdUsers.add(saveUser(
                    "user" + i,
                    "user" + i + "@gmail.com",
                    "Member " + i,
                    Set.of(userRole)));
        }
        return createdUsers;
    }

    private User saveUser(String username, String email, String displayName, Set<Role> roles) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("123456")) // Pass chung: 123456
                .displayName(displayName)
                .isActive(true)
                .isEmailVerified(true)
                .birthDate(LocalDate.of(2000, 1, 1))
                .roles(roles)
                .build();
        return userRepository.save(user);
    }

    private void createServerWithMembers(User owner, String serverName, String description, List<User> membersToAdd) {
        // 1. Lưu Server
        Server server = new Server();
        server.setName(serverName);
        server.setDescription(description);
        server.setOwner(owner);
        server.setStatus(ServerStatus.ACTIVE);
        server = serverRepository.save(server);

        // 2. Tạo 3 Channels mặc định
        createChannel(server, "general", ChannelType.TEXT);
        createChannel(server, "chat-dev", ChannelType.TEXT);
        createChannel(server, "Lobby Voice", ChannelType.VOICE);

        // 3. Add Owner vào ServerMember
        addMemberToServer(server, owner);

        // 4. Add các thành viên khác vào ServerMember
        for (User member : membersToAdd) {
            addMemberToServer(server, member);
        }
    }

    private void createChannel(Server server, String name, ChannelType type) {
        Channel channel = new Channel();
        channel.setName(name);
        channel.setType(type);
        channel.setServer(server);
        channel.setIsPrivate(false);
        channelRepository.save(channel);
    }

    private void addMemberToServer(Server server, User user) {
        ServerMember member = new ServerMember();
        member.setServer(server);
        member.setUser(user);
        member.setNickname(user.getDisplayName());
        member.setIsBanned(false);
        serverMemberRepository.save(member);
    }
}