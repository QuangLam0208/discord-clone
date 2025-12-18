package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Category;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Override
    public Category createCategory(Long serverId, String name) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new RuntimeException("Server not found"));

        Category category = new Category();
        category.setServer(server);
        category.setName(name);

        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Long categoryId, String newName) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setName(newName);
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        // Lưu ý: Nếu trong Category có channel, JPA cascade=ALL sẽ xóa luôn channel đó
        // Nếu muốn giữ channel, bạn phải set category của channel đó = null trước khi xóa
        if (categoryRepository.existsById(categoryId)) {
            categoryRepository.deleteById(categoryId);
        }
    }

    @Override
    public List<Category> getCategoriesByServer(Long serverId) {
        return categoryRepository.findByServerId(serverId);
    }
}