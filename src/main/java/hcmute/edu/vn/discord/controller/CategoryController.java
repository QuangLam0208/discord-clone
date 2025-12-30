package hcmute.edu.vn.discord.controller;

import hcmute.edu.vn.discord.dto.request.CategoryRequest;
import hcmute.edu.vn.discord.dto.response.CategoryResponse;
import hcmute.edu.vn.discord.entity.jpa.Category;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.service.CategoryService;
import hcmute.edu.vn.discord.service.ServerService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ServerService serverService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request,
                                                           Principal principal) {
        request.normalize();

        // Xác nhận server tồn tại
        Server server = serverService.getServerById(request.getServerId());
        // TODO: Có thể bổ sung kiểm tra quyền: principal phải là owner hoặc có quyền MANAGE_CHANNELS

        Category created = categoryService.createCategory(request.getServerId(), request.getName());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(CategoryResponse.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id,
                                                           @Valid @RequestBody CategoryRequest request,
                                                           Principal principal) {
        request.normalize();

        // Không cho phép đổi serverId khi cập nhật
        Category existing = categoryService.getCategoryById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        if (request.getServerId() != null && !request.getServerId().equals(existing.getServer().getId())) {
            throw new IllegalArgumentException("Không thể thay đổi server của category");
        }

        // TODO: Có thể bổ sung kiểm tra quyền: principal phải là owner hoặc có quyền MANAGE_CHANNELS

        Category updated = categoryService.updateCategory(id, request.getName());
        return ResponseEntity.ok(CategoryResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, Principal principal) {
        // TODO: Có thể bổ sung kiểm tra quyền: principal phải là owner hoặc có quyền MANAGE_CHANNELS
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(c -> ResponseEntity.ok(CategoryResponse.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/server/{serverId}")
    public ResponseEntity<List<CategoryResponse>> getCategoriesByServer(@PathVariable Long serverId) {
        // Nếu muốn chỉ thành viên server được xem, thêm @PreAuthorize("isAuthenticated()")
        Server server = serverService.getServerById(serverId);
        List<CategoryResponse> data = categoryService.getCategoriesByServer(serverId).stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(data);
    }
}