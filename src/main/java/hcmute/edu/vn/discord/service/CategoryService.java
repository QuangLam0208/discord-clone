package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.CategoryRequest; // Dùng Request DTO cho gọn tham số
import hcmute.edu.vn.discord.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long categoryId, CategoryRequest request);
    void deleteCategory(Long categoryId);
    List<CategoryResponse> getCategoriesByServer(Long serverId);
    CategoryResponse getCategoryById(Long id);
}