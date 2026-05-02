package tea4life.product_service.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.dto.request.NewsCategoryRequest;
import tea4life.product_service.dto.request.NewsRequest;
import tea4life.product_service.dto.response.NewsCategoryResponse;
import tea4life.product_service.model.NewsCategory;
import tea4life.product_service.repository.news.NewsCategoryRepository;
import tea4life.product_service.service.NewsCategoryAdminService;
import tea4life.product_service.utils.NewsMapper;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 3:17 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.service.impl
 */

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsCategoryAdminServiceImpl implements NewsCategoryAdminService {
    private final NewsCategoryRepository newsCategoryRepository;
    private final NewsMapper newsMapper;

    public List<NewsCategoryResponse> findAllNewsCategory() {
        return newsCategoryRepository.findAll().stream()
                .map(newsMapper::mapToCategoryResponse).toList();
    }

    public NewsCategoryResponse findById(Long id) {
        return newsCategoryRepository.findById(id)
                .map(newsMapper::mapToCategoryResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục tin tức!"));
    }

    public NewsCategoryResponse findBySlug(String slug) {
        return newsCategoryRepository.findBySlug(slug)
                .map(newsMapper::mapToCategoryResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục tin tức!"));
    }

    @Transactional
    public NewsCategoryResponse create(NewsCategoryRequest request) {
        // Luồng Create: Khởi tạo Entity mới tinh, bỏ qua hoàn toàn khái niệm ID
        NewsCategory newsCategory = new NewsCategory();
        return processAndSaveNewsCategory(newsCategory, request);
    }

    @Transactional
    public NewsCategoryResponse update(Long id, NewsCategoryRequest request) {
        // Luồng Update: Bắt buộc ID phải tồn tại, nếu sai ném lỗi 404 ngay lập tức
        NewsCategory newsCategory = newsCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục để cập nhật!"));

        return processAndSaveNewsCategory(newsCategory, request);
    }

    @Transactional
    public void delete(Long id) {
        NewsCategory newsCategory = newsCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục để xóa!"));
        newsCategoryRepository.delete(newsCategory);
    }

    private NewsCategoryResponse processAndSaveNewsCategory(NewsCategory newsCategory, NewsCategoryRequest request) {
        newsCategory.setName(request.name());

        newsCategory = newsCategoryRepository.save(newsCategory);
        return newsMapper.mapToCategoryResponse(newsCategory);
    }
}
