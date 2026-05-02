package tea4life.product_service.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.response.NewsCategoryResponse;
import tea4life.product_service.service.NewsCategoryService;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 5:58 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.controller
 */

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/public/news-categories")
public class NewsCategoryController {
    NewsCategoryService newsCategoryService;

    @GetMapping
    public ApiResponse<List<NewsCategoryResponse>> getAll() {
        return new ApiResponse<>(newsCategoryService.findAllNewsCategory());
    }

    @GetMapping("/{slug}")
    public ApiResponse<NewsCategoryResponse> getBySlug(@PathVariable String slug) {
        return new ApiResponse<>(newsCategoryService.findBySlug(slug));
    }
}
