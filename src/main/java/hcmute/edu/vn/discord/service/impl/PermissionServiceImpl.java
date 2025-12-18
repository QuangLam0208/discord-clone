package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Permission;
import hcmute.edu.vn.discord.repository.PermissionRepository;
import hcmute.edu.vn.discord.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public Permission createPermission(Permission permission) {
        // ensure id is null so JPA will insert
        permission.setId(null);
        return permissionRepository.save(permission);
    }

    @Override
    public Permission updatePermission(Long id, Permission updatedPermission) {
        Permission existing = permissionRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Permission not found with id " + id));

        // only allow updating mutable fields
        existing.setDescription(updatedPermission.getDescription());
        // code is updatable? in entity it's updatable = false, so avoid changing it

        return permissionRepository.save(existing);
    }

    @Override
    public void deletePermission(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new RuntimeException("Permission not found with id " + id);
        }
        permissionRepository.deleteById(id);
    }

    @Override
    public Optional<Permission> getPermissionById(Long id) {
        return permissionRepository.findById(id);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    public Optional<Permission> findByCode(String code) {
        return permissionRepository.findByCode(code);
    }
}

