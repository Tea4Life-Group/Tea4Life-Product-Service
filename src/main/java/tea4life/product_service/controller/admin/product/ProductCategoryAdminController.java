package tea4life.product_service.controller.admin.product;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.*;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.request.CreateProductCategoryRequest;
import tea4life.product_service.dto.response.ProductCategoryResponse;
import tea4life.product_service.service.ProductCategoryAdminService;

import java.util.List;

/**
 * Admin 2/25/2026
 *
 **/
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/product-categories")
public class ProductCategoryAdminController {

    ProductCategoryAdminService productCategoryAdminService;

    @PostMapping()
    public ApiResponse<ProductCategoryResponse> createCategory(
            @RequestBody @Valid CreateProductCategoryRequest request
    ) {
        return new ApiResponse<>(productCategoryAdminService.createCategory(request));
    }

    @GetMapping()
    public ApiResponse<List<ProductCategoryResponse>> findAllCategories() {
        return new ApiResponse<>(productCategoryAdminService.findAllCategories());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductCategoryResponse> findCategoryById(
            @PathVariable("id") Long id
    ) {
        return new ApiResponse<>(productCategoryAdminService.findCategoryById(id));
    }

    @PostMapping("/{id}")
    public ApiResponse<ProductCategoryResponse> updateCategory(
            @PathVariable("id") Long id,
            @RequestBody @Valid CreateProductCategoryRequest request
    ) {
        return new ApiResponse<>(productCategoryAdminService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<@NonNull Void> deleteCategory(
            @PathVariable("id") Long id
    ) {
        productCategoryAdminService.deleteCategory(id);
        return ApiResponse.<Void>builder().build();
    }
}
