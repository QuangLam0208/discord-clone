package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.dto.request.CategoryRequest;
import hcmute.edu.vn.discord.entity.jpa.Category;
import java.util.List;

public interface CategoryService {
    Category createCategory(CategoryRequest request);
    Category updateCategory(Long categoryId, CategoryRequest request);
    void deleteCategory(Long categoryId);
    List<Category> getCategoriesByServer(Long serverId);
    Category getCategoryById(Long id);
}