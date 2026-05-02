package tea4life.product_service.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.response.*;
import tea4life.product_service.model.News;
import tea4life.product_service.repository.news.NewsRepository;
import tea4life.product_service.service.NewsService;
import tea4life.product_service.utils.NewsMapper;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:27 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.service.impl
 */

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsServiceImpl implements NewsService {
    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    public PageResponse<NewsSummaryResponse> findAll(Pageable pageable) {
        Page<@NonNull NewsSummaryResponse> responsePage = newsRepository
                .findAllNewsWithCategory(pageable)
                .map(newsMapper::mapToSummaryResponse);
        return new PageResponse<>(responsePage);
    }

    public List<NewsSummaryResponse> findTop3LatestNews() {
        return newsRepository.findTop3ByOrderByCreatedAtDesc()
                .stream()
                .map(newsMapper::mapToSummaryResponse)
                .toList();
    }

    public NewsDetailResponse findBySlug(String slug) {
        News news = newsRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết!"));
        return newsMapper.mapToDetailResponse(news);
    }

    public PageResponse<NewsSummaryResponse> findByCategorySlug(String categorySlug, Pageable pageable) {
        Page<@NonNull NewsSummaryResponse> responsePage = newsRepository
                .findAllByCategorySlug(categorySlug, pageable)
                .map(newsMapper::mapToSummaryResponse);
        return new PageResponse<>(responsePage);
    }
}