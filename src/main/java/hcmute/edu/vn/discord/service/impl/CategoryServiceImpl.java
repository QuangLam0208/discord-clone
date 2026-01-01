package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.CategoryRequest;
import hcmute.edu.vn.discord.entity.jpa.Category;
import hcmute.edu.vn.discord.entity.jpa.Channel;
import hcmute.edu.vn.discord.entity.jpa.Server;
import hcmute.edu.vn.discord.repository.CategoryRepository;
import hcmute.edu.vn.discord.repository.ChannelRepository;
import hcmute.edu.vn.discord.repository.ServerRepository;
import hcmute.edu.vn.discord.service.CategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;

    @Override
    @Transactional
    public Category createCategory(CategoryRequest request) {
        Server server = serverRepository.findById(request.getServerId())
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));

        Category category = new Category();
        category.setServer(server);
        category.setName(request.getName());

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        // Logic check serverId không đổi (giữ nguyên từ code cũ)
        if (request.getServerId() != null && !request.getServerId().equals(category.getServer().getId())) {
            throw new IllegalArgumentException("Không thể thay đổi server của category");
        }

        category.setName(request.getName());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Category not found");
        }

        List<Channel> channels = channelRepository.findByCategoryId(categoryId);
        for (Channel channel : channels) {
            channel.setCategory(null); // Kênh sẽ nhảy ra ngoài, không bị xóa theo
        }
        channelRepository.saveAll(channels);

        // Sau đó mới xóa Category
        categoryRepository.deleteById(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategoriesByServer(Long serverId) {
        return categoryRepository.findByServerId(serverId);
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }
}