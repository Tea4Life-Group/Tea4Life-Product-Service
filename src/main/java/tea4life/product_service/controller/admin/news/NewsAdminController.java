package tea4life.product_service.controller.admin.news;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.NewsRequest;
import tea4life.product_service.dto.response.NewsDetailResponse;
import tea4life.product_service.dto.response.NewsSummaryResponse;
import tea4life.product_service.service.NewsAdminService;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:03 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.controller.admin.news
 */

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/news")
public class NewsAdminController {
    NewsAdminService newsService;

    @GetMapping
    public ApiResponse<PageResponse<NewsSummaryResponse>> getAll(Pageable pageable) {
        return new ApiResponse<>(newsService.findAll(pageable));
    }

    @GetMapping("/{id}") // Admin thường sửa bằng ID
    public ApiResponse<NewsDetailResponse> getById(@PathVariable Long id) {
        return new ApiResponse<>(newsService.findById(id));
    }

    @PostMapping
    public ApiResponse<NewsDetailResponse> create(@Valid @RequestBody NewsRequest request) {
        return new ApiResponse<>(newsService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<NewsDetailResponse> update(@PathVariable Long id, @Valid @RequestBody NewsRequest request) {
        return new ApiResponse<>(newsService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
