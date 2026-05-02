package tea4life.product_service.controller.admin.news;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.request.NewsCategoryRequest;
import tea4life.product_service.dto.response.NewsCategoryResponse;
import tea4life.product_service.service.NewsCategoryAdminService;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:04 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.controller.admin.news
 */

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/news-categories")
public class NewsCategoryAdminController {
    NewsCategoryAdminService newsCategoryService;

    @GetMapping
    public ApiResponse<List<NewsCategoryResponse>> getAll() {
        return new ApiResponse<>(newsCategoryService.findAllNewsCategory());
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsCategoryResponse> getById(@PathVariable Long id) {
        return new ApiResponse<>(newsCategoryService.findById(id));
    }

    @PostMapping
    public ApiResponse<NewsCategoryResponse> create(@Valid @RequestBody NewsCategoryRequest request) {
        // Trả về 201 CREATED khi tạo mới thành công
        return new ApiResponse<>(newsCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<NewsCategoryResponse> update(@PathVariable Long id, @Valid @RequestBody NewsCategoryRequest request) {
        return new ApiResponse<>(newsCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsCategoryService.delete(id);
        return ResponseEntity.noContent().build(); // Trả về 204 NO CONTENT
    }
}
