package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.RegisterRequest;
import hcmute.edu.vn.discord.entity.enums.ERole;
import hcmute.edu.vn.discord.entity.jpa.Role;
import hcmute.edu.vn.discord.entity.jpa.User;
import hcmute.edu.vn.discord.repository.RoleRepository;
import hcmute.edu.vn.discord.repository.UserRepository;
import hcmute.edu.vn.discord.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

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
}