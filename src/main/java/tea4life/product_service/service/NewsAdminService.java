package tea4life.product_service.service;

import org.springframework.data.domain.Pageable;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.NewsRequest;
import tea4life.product_service.dto.response.NewsDetailResponse;
import tea4life.product_service.dto.response.NewsSummaryResponse;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:27 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.service
 */
public interface NewsAdminService {
    PageResponse<NewsSummaryResponse> findAll(Pageable pageable);
    PageResponse<NewsSummaryResponse> findByCategoryId(Long categoryId, Pageable pageable);
    NewsDetailResponse findById(Long id);
    NewsDetailResponse findBySlug(String slug);
    NewsDetailResponse create(NewsRequest request);
    NewsDetailResponse update(Long id, NewsRequest request);
    void delete(Long id);
}
