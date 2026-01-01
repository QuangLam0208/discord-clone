package hcmute.edu.vn.discord.config;

import hcmute.edu.vn.discord.dto.request.MessageRequest;
import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.enums.ERole;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.service.FriendService;
import hcmute.edu.vn.discord.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final PermissionRepository permissionRepository;
    private final CategoryRepository categoryRepository;
    // service để seed friend và messages
    private final FriendService friendService;
    private final MessageService messageService;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            System.out.println(">>> BỎ QUA SEED: DB đã có dữ liệu người dùng.");
            return;
        }

        System.out.println(">>> ĐANG KHỞI TẠO DỮ LIỆU MẪU (SEEDING)...");

        createPermissionsIfNotExist();
        createRolesIfNotExist();
        List<User> users = createUsers();

        Server s1 = createServerWithMembers(users.get(0), "Admin's Server", "Server riêng tư", List.of());
        Server s2 = createServerWithMembers(users.get(1), "Duo Gaming", "Chơi game cặp", List.of(users.get(2)));
        Server s3 = createServerWithMembers(users.get(3), "Trio Coding", "Hội lập trình 3 người", List.of(users.get(4), users.get(5)));
        Server s4 = createServerWithMembers(users.get(6), "Squad Community", "Cộng đồng lớn", List.of(users.get(7), users.get(8), users.get(9)));

        createDefaultCategoriesAndAssignChannels(s1);
        createDefaultCategoriesAndAssignChannels(s2);
        createDefaultCategoriesAndAssignChannels(s3);
        createDefaultCategoriesAndAssignChannels(s4);

        seedFriendRelations(users);

        seedChannelMessages(s1);
        seedChannelMessages(s2);
        seedChannelMessages(s3);
        seedChannelMessages(s4);

        System.out.println(">>> KHỞI TẠO DỮ LIỆU THÀNH CÔNG! <<<");
    }

    private void createPermissionsIfNotExist() {
        for (EPermission ePerm : EPermission.values()) {
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
            roleRepository.save(new Role(null, ERole.ADMIN, new HashSet<>()));
            roleRepository.save(new Role(null, ERole.USER_DEFAULT, new HashSet<>()));
            roleRepository.save(new Role(null, ERole.USER_PREMIUM, new HashSet<>()));
        }

        Role defaultRole = roleRepository.findByName(ERole.USER_DEFAULT).orElse(null);
        if (defaultRole != null) {
            Permission addReaction = permissionRepository
                    .findByCode(EPermission.ADD_REACTIONS.getCode()).orElse(null);
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

        createdUsers.add(saveUser("admin", "admin@gmail.com", "System Admin", Set.of(adminRole, userRole)));
        for (int i = 1; i <= 9; i++) {
            createdUsers.add(saveUser("user" + i, "user" + i + "@gmail.com", "Member " + i, Set.of(userRole)));
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

    private Server createServerWithMembers(User owner, String serverName, String description, List<User> membersToAdd) {
        Server server = new Server();
        server.setName(serverName);
        server.setDescription(description);
        server.setOwner(owner);
        server.setStatus(ServerStatus.ACTIVE);
        server = serverRepository.save(server);

        Channel ch1 = createChannel(server, "general", ChannelType.TEXT);
        Channel ch2 = createChannel(server, "chat-dev", ChannelType.TEXT);
        Channel ch3 = createChannel(server, "Lobby Voice", ChannelType.VOICE);

        addMemberToServer(server, owner);
        for (User member : membersToAdd) {
            addMemberToServer(server, member);
        }

        return server;
    }

    private Channel createChannel(Server server, String name, ChannelType type) {
        Channel channel = new Channel();
        channel.setName(name);
        channel.setType(type);
        channel.setServer(server);
        channel.setIsPrivate(false);
        return channelRepository.save(channel);
    }

    private void addMemberToServer(Server server, User user) {
        ServerMember member = new ServerMember();
        member.setServer(server);
        member.setUser(user);
        member.setNickname(user.getDisplayName());
        member.setIsBanned(false);
        serverMemberRepository.save(member);
    }

    private void createDefaultCategoriesAndAssignChannels(Server server) {
        Category textCate = new Category();
        textCate.setName("Text Channels");
        textCate.setServer(server);
        textCate = categoryRepository.save(textCate);

        Category voiceCate = new Category();
        voiceCate.setName("Voice Channels");
        voiceCate.setServer(server);
        voiceCate = categoryRepository.save(voiceCate);

        List<Channel> channels = channelRepository.findByServerIdOrderByIdAsc(server.getId());
        for (Channel channel : channels) {
            if (channel.getType() == ChannelType.TEXT) {
                channel.setCategory(textCate);
            } else if (channel.getType() == ChannelType.VOICE) {
                channel.setCategory(voiceCate);
            }
        }
        channelRepository.saveAll(channels);
    }

    private void seedFriendRelations(List<User> users) {
        // Tạo một số quan hệ bạn bè để màn Friends/DM có dữ liệu
        // user1 <-> user2 (accept)
        try {
            var fr = friendService.sendRequest(users.get(1).getId(), users.get(2).getId());
            friendService.acceptRequest(fr.getId(), users.get(2).getId());
        } catch (Exception e) {
            System.out.println("Seed friend (user1-user2) skipped: " + e.getMessage());
        }

        // user3 -> user4 (inbound pending với user4)
        try {
            friendService.sendRequest(users.get(3).getId(), users.get(4).getId());
        } catch (Exception e) {
            System.out.println("Seed friend (user3->user4) skipped: " + e.getMessage());
        }

        // user5 <-> user6 (accept)
        try {
            var fr = friendService.sendRequest(users.get(5).getId(), users.get(6).getId());
            friendService.acceptRequest(fr.getId(), users.get(6).getId());
        } catch (Exception e) {
            System.out.println("Seed friend (user5-user6) skipped: " + e.getMessage());
        }
    }

    private void seedChannelMessages(Server server) {
        // Tạo 5 tin nhắn mẫu trong channel TEXT đầu tiên của server (nếu có MessageService)
        List<Channel> textChannels = channelRepository.findByServerIdAndType(server.getId(), ChannelType.TEXT);
        if (textChannels.isEmpty()) return;

        Channel target = textChannels.get(0);
        // Lấy danh sách member để chọn người gửi
        List<ServerMember> members = serverMemberRepository.findByServerId(server.getId());
        if (members.isEmpty()) return;

        List<User> senders = new ArrayList<>();
        for (ServerMember m : members) {
            if (m.getUser() != null) senders.add(m.getUser());
        }
        if (senders.isEmpty()) return;

        // Gửi 5 tin nhắn theo thứ tự sender xoay vòng
        String[] contents = new String[]{
                "Chào mọi người 👋",
                "Dự án Discord clone ngày 1: hoàn thiện auth + layout.",
                "Nhớ merge vào dev giữa ngày nhé!",
                "Realtime chat đã chạy, test 2 tab trình duyệt.",
                "Upload ảnh OK, please check /files/*."
        };

        for (int i = 0; i < contents.length; i++) {
            User sender = senders.get(i % senders.size());
            try {
                // MessageService.createMessage sẽ lưu JPA/Mongo tùy bạn triển khai
                MessageRequest req = new MessageRequest();
                req.setContent(contents[i]);
                req.setReplyToId(null);
                req.setAttachments(Collections.emptyList()); // Không có ảnh ở seed mẫu
                messageService.createMessage(target.getId(), sender.getUsername(), req);
            } catch (Exception e) {
                System.out.println("Seed message error: " + e.getMessage());
            }
        }

        // Kiểm tra lấy lịch sử để chắc chắn dữ liệu xuất hiện
        try {
            var list = messageService.getMessagesByChannel(
                    target.getId(),
                    senders.get(0).getUsername(),
                    PageRequest.of(0, 20, Sort.by("createdAt").descending())
            );
            System.out.printf("Seeded %d messages in channel #%s (server %s)%n",
                    list.size(), target.getName(), server.getName());
        } catch (Exception e) {
            System.out.println("Verify messages failed: " + e.getMessage());
        }
    }
}