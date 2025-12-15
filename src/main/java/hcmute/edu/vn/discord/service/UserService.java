package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.User;
import java.util.Optional;

public interface UserService {
    User registerUser(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    boolean existsByEmail(String email);
}