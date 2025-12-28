package hcmute.edu.vn.discord.service;

import hcmute.edu.vn.discord.entity.jpa.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryService {
    Category createCategory(Long serverId, String name);
    Category updateCategory(Long categoryId, String newName);
    void deleteCategory(Long categoryId);
    List<Category> getCategoriesByServer(Long serverId);
    Optional<Category> getCategoryById(Long id);
}