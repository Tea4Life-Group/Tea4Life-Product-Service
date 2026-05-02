package tea4life.product_service.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.dto.response.NewsCategoryResponse;
import tea4life.product_service.repository.news.NewsCategoryRepository;
import tea4life.product_service.utils.NewsMapper;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 6:10 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.service.impl
 */

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsCategoryServiceImpl implements tea4life.product_service.service.NewsCategoryService {
    private final NewsCategoryRepository newsCategoryRepository;
    private final NewsMapper newsMapper;

    @Override
    public List<NewsCategoryResponse> findAllNewsCategory() {
        return newsCategoryRepository.findAll().stream()
                .map(newsMapper::mapToCategoryResponse).toList();
    }

    @Override
    public NewsCategoryResponse findBySlug(String slug) {
        return newsCategoryRepository.findBySlug(slug)
                .map(newsMapper::mapToCategoryResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục tin tức!"));
    }
}
