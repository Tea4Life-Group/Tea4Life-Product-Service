package tea4life.product_service.service;

import org.springframework.data.domain.Pageable;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.response.NewsDetailResponse;
import tea4life.product_service.dto.response.NewsSummaryResponse;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:26 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.service
 */
public interface NewsService {
    PageResponse<NewsSummaryResponse> findAll(Pageable pageable);
    NewsDetailResponse findBySlug(String slug);
    PageResponse<NewsSummaryResponse> findByCategorySlug(String categorySlug, Pageable pageable);
    List<NewsSummaryResponse> findTop3LatestNews();
}
