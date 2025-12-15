package hcmute.edu.vn.discord.repository;

import hcmute.edu.vn.discord.entity.enums.ERole;
import hcmute.edu.vn.discord.entity.jpa.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    // Tìm Role theo tên Enum (quan trọng để gán USER_DEFAULT)
    Optional<Role> findByName(ERole name);
}