package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionService {
    Permission createPermission(Permission permission);
    Permission updatePermission(Long id, Permission permission);
    void deletePermission(Long id);
    Optional<Permission> getPermissionById(Long id);
    List<Permission> getAllPermissions();
    Optional<Permission> findByCode(String code);
}
