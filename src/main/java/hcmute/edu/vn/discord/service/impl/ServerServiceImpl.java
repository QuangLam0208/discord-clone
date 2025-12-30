package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.ServerRequest;
import hcmute.edu.vn.discord.dto.response.*;
import hcmute.edu.vn.discord.entity.enums.ChannelType;
import hcmute.edu.vn.discord.entity.enums.EPermission;
import hcmute.edu.vn.discord.entity.enums.ServerStatus;
import hcmute.edu.vn.discord.entity.jpa.*;
import hcmute.edu.vn.discord.repository.*;
import hcmute.edu.vn.discord.service.ChannelService;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private static final Logger log = LoggerFactory.getLogger(ServerServiceImpl.class);

    private final ServerRepository serverRepository;
    private final ServerRoleRepository serverRoleRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final CategoryRepository categoryRepository;
    private final ChannelService channelService; // Inject để dùng logic lấy channel có filter

    @Override
    @Transactional
    public ServerResponse createServer(ServerRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 1. Tạo Server
        Server server = new Server();
        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setIconUrl(request.getIconUrl());
        server.setOwner(owner);
        server.setStatus(ServerStatus.ACTIVE);
        Server savedServer = serverRepository.save(server);

        // 2. Khởi tạo Quyền và Roles
        Map<EPermission, Permission> permMap = bootstrapPermissions();

        // Role @everyone
        ServerRole everyoneRole = new ServerRole();
        everyoneRole.setName("@everyone");
        everyoneRole.setPriority(0);
        everyoneRole.setServer(savedServer);
        everyoneRole.setPermissions(Set.of(
                permMap.get(EPermission.VIEW_CHANNELS),
                permMap.get(EPermission.SEND_MESSAGES),
                permMap.get(EPermission.READ_MESSAGE_HISTORY),
                permMap.get(EPermission.CREATE_INVITE)
        ));
        serverRoleRepository.save(everyoneRole);

        // Role Admin (Owner)
        ServerRole adminRole = new ServerRole();
        adminRole.setName("Owner");
        adminRole.setPriority(100);
        adminRole.setServer(savedServer);
        adminRole.setPermissions(new HashSet<>(permMap.values()));
        ServerRole savedAdminRole = serverRoleRepository.save(adminRole);

        // 3. Tạo cấu trúc mặc định: Category General -> Channel general
        Category defaultCategory = new Category();
        defaultCategory.setName("General");
        defaultCategory.setServer(savedServer);
        Category savedCategory = categoryRepository.save(defaultCategory);

        Channel generalChannel = new Channel();
        generalChannel.setName("general");
        generalChannel.setType(ChannelType.TEXT);
        generalChannel.setServer(savedServer);
        generalChannel.setCategory(savedCategory);
        generalChannel.setIsPrivate(false);
        channelRepository.save(generalChannel);

        // 4. Đưa Owner vào danh sách thành viên
        ServerMember ownerMember = new ServerMember();
        ownerMember.setServer(savedServer);
        ownerMember.setUser(owner);
        ownerMember.setNickname(owner.getDisplayName());
        ownerMember.setJoinedAt(LocalDateTime.now());
        ownerMember.setIsBanned(false);
        ownerMember.setRoles(new HashSet<>(Set.of(savedAdminRole)));
        serverMemberRepository.save(ownerMember);

        log.info("Server created: {} by {}", savedServer.getName(), username);
        return ServerResponse.from(savedServer);
    }

    @Override
    @Transactional
    public ServerResponse updateServer(Long id, ServerRequest request) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));

        server.setName(request.getName());
        server.setDescription(request.getDescription());
        server.setIconUrl(request.getIconUrl());

        return ServerResponse.from(serverRepository.save(server));
    }

    @Override
    @Transactional(readOnly = true)
    public ServerDetailResponse getServerDetail(Long id) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server không tồn tại"));

        // Lấy dữ liệu tổng hợp
        List<CategoryResponse> categories = categoryRepository.findByServerId(id).stream()
                .map(CategoryResponse::from).toList();

        // Tận dụng ChannelService để lọc kênh private theo quyền user hiện tại
        List<ChannelResponse> channels = channelService.getChannelsByServer(id);

        List<ServerMemberResponse> members = serverMemberRepository.findByServerId(id).stream()
                .map(ServerMemberResponse::from).toList();

        return ServerDetailResponse.builder()
                .serverInfo(ServerResponse.from(server))
                .categories(categories)
                .channels(channels)
                .members(members)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Server> getAllServers() {
        return serverRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Server getServerById(Long id) {
        return serverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Server> getServersByCurrentUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return serverRepository.findByMemberUsername(username);
    }

    @Override
    @Transactional
    public void deleteServer(Long serverId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));

        if (!server.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Chỉ chủ sở hữu mới có quyền xóa server");
        }

        serverRepository.delete(server);
        log.info("Server deleted: {} by {}", serverId, username);
    }

    private Map<EPermission, Permission> bootstrapPermissions() {
        Map<EPermission, Permission> map = new EnumMap<>(EPermission.class);
        for (EPermission ep : EPermission.values()) {
            Permission perm = permissionRepository.findByCode(ep.name())
                    .orElseGet(() -> permissionRepository.save(new Permission(null, ep.name(), ep.getDescription())));
            map.put(ep, perm);
        }
        return map;
    }
}