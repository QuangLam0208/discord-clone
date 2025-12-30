package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.CategoryRequest;
import hcmute.edu.vn.discord.dto.response.CategoryResponse;
import hcmute.edu.vn.discord.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // 1. TẠO CATEGORY: Cần quyền MANAGE_CHANNELS
    @PostMapping
    @PreAuthorize("@serverAuth.canManageChannels(#request.serverId, authentication.name)")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        request.normalize();
        CategoryResponse created = categoryService.createCategory(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    // 2. SỬA CATEGORY: Cần quyền MANAGE_CHANNELS (Check theo ID category -> ra serverID)
    @PutMapping("/{id}")
    @PreAuthorize("@serverAuth.canManageChannels(@serverAuth.serverIdOfCategory(#id), authentication.name)")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                                                           @Valid @RequestBody CategoryRequest request) {
        request.normalize();
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    // 3. XÓA CATEGORY: Cần quyền MANAGE_CHANNELS
    @DeleteMapping("/{id}")
    @PreAuthorize("@serverAuth.canManageChannels(@serverAuth.serverIdOfCategory(#id), authentication.name)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // 4. XEM 1 CATEGORY: Chỉ cần là Member
    @GetMapping("/{id}")
    @PreAuthorize("@serverAuth.isMember(@serverAuth.serverIdOfCategory(#id), authentication.name)")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    // 5. XEM LIST CATEGORY: Chỉ cần là Member
    @GetMapping("/server/{serverId}")
    @PreAuthorize("@serverAuth.isMember(#serverId, authentication.name)")
    public ResponseEntity<List<CategoryResponse>> getCategoriesByServer(@PathVariable Long serverId) {
        return ResponseEntity.ok(categoryService.getCategoriesByServer(serverId));
    }
}