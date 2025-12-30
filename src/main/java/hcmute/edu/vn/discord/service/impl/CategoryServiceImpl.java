package hcmute.edu.vn.discord.service.impl;

import hcmute.edu.vn.discord.dto.request.CategoryRequest;
import hcmute.edu.vn.discord.dto.response.CategoryResponse;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ServerRepository serverRepository;
    private final ChannelRepository channelRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Server server = serverRepository.findById(request.getServerId())
                .orElseThrow(() -> new EntityNotFoundException("Server not found"));

        Category category = new Category();
        category.setServer(server);
        category.setName(request.getName());

        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.from(savedCategory); // Convert ngay tại đây
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        // Logic check serverId không đổi có thể để ở Controller hoặc Service đều được
        if (request.getServerId() != null && !request.getServerId().equals(category.getServer().getId())) {
            throw new IllegalArgumentException("Không thể thay đổi server của category");
        }

        category.setName(request.getName());
        return CategoryResponse.from(categoryRepository.save(category));
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
    public List<CategoryResponse> getCategoriesByServer(Long serverId) {
        // Convert List<Entity> -> List<DTO>
        return categoryRepository.findByServerId(serverId).stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));
        return CategoryResponse.from(category);
    }
}