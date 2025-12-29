package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.entity.jpa.Category;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ServerRepository serverRepository;

    @Override
    @Transactional
    public Category createCategory(Long serverId, String name) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));
        Category category = new Category();
        category.setServer(server);
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Long categoryId, String newName) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        category.setName(newName);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        // Lưu ý: Nếu trong Category có channel, JPA cascade=ALL sẽ xóa luôn channel đó
        // Nếu muốn giữ channel, phải set category của channel đó = null trước khi xóa
        if (categoryRepository.existsById(categoryId)) {
            categoryRepository.deleteById(categoryId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategoriesByServer(Long serverId) {
        return categoryRepository.findByServerId(serverId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
}