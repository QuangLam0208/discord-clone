package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.CategoryRequest;
import hcmute.edu.vn.discord.dto.response.CategoryResponse;
import hcmute.edu.vn.discord.entity.jpa.Category;
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
        Category created = categoryService.createCategory(request);
        CategoryResponse response = CategoryResponse.from(created);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    // 2. SỬA CATEGORY: Cần quyền MANAGE_CHANNELS (Check theo ID category -> ra serverID)
    @PutMapping("/{id}")
    @PreAuthorize("@serverAuth.canManageChannels(@serverAuth.serverIdOfCategory(#id), authentication.name)")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                                                           @Valid @RequestBody CategoryRequest request) {
        request.normalize();
        Category updated = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(CategoryResponse.from(updated));
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
        Category category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(CategoryResponse.from(category));
    }

    // 5. XEM LIST CATEGORY: Chỉ cần là Member
    @GetMapping("/server/{serverId}")
    @PreAuthorize("@serverAuth.isMember(#serverId, authentication.name)")
    public ResponseEntity<List<CategoryResponse>> getCategoriesByServer(@PathVariable Long serverId) {
        List<CategoryResponse> categories = categoryService.getCategoriesByServer(serverId)
                .stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(categories);
    }
}