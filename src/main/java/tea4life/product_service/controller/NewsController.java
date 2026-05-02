package tea4life.product_service.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.response.NewsDetailResponse;
import tea4life.product_service.dto.response.NewsSummaryResponse;
import tea4life.product_service.service.NewsService;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:02 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.controller
 */

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/public/news")
public class NewsController {
    NewsService newsService;

    // FE gọi trang chủ: /api/v1/news?page=0&size=10
    @GetMapping
    public ApiResponse<PageResponse<NewsSummaryResponse>> getAll(Pageable pageable) {
        return new ApiResponse<>(newsService.findAll(pageable));
    }

    // FE gọi khi khách click vào đọc bài
    @GetMapping("/{slug}")
    public ApiResponse<NewsDetailResponse> getBySlug(@PathVariable String slug) {
        return new ApiResponse<>(newsService.findBySlug(slug));
    }

    // FE gọi khi khách bấm vào một Danh mục trên thanh menu
    @GetMapping("/category/{categorySlug}")
    public ApiResponse<PageResponse<NewsSummaryResponse>> getByCategorySlug(
            @PathVariable String categorySlug,
            Pageable pageable) {
        return new ApiResponse<>(newsService.findByCategorySlug(categorySlug, pageable));
    }

    @GetMapping("/latest")
    public ApiResponse<List<NewsSummaryResponse>> getLatestNews() {
        return new ApiResponse<>(newsService.findTop3LatestNews());
    }
}
