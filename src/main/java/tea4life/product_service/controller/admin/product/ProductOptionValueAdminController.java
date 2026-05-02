package tea4life.product_service.controller.admin.product;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.*;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.CreateProductOptionValueRequest;
import tea4life.product_service.dto.response.ProductOptionValueResponse;
import tea4life.product_service.service.ProductOptionValueAdminService;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/product-options/{productOptionId}/values")
public class ProductOptionValueAdminController {

    ProductOptionValueAdminService productOptionValueAdminService;

    @PostMapping()
    public ApiResponse<ProductOptionValueResponse> createValue(
            @PathVariable("productOptionId") Long productOptionId,
            @RequestBody @Valid CreateProductOptionValueRequest request
    ) {
        return new ApiResponse<>(productOptionValueAdminService.createValue(productOptionId, request));
    }

    @GetMapping()
    public ApiResponse<PageResponse<ProductOptionValueResponse>> findAllValues(
            @PathVariable("productOptionId") Long productOptionId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return new ApiResponse<>(productOptionValueAdminService.findAllValues(productOptionId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductOptionValueResponse> findValueById(
            @PathVariable("productOptionId") Long productOptionId,
            @PathVariable("id") Long id
    ) {
        return new ApiResponse<>(productOptionValueAdminService.findValueById(productOptionId, id));
    }

    @PostMapping("/{id}")
    public ApiResponse<ProductOptionValueResponse> updateValue(
            @PathVariable("productOptionId") Long productOptionId,
            @PathVariable("id") Long id,
            @RequestBody @Valid CreateProductOptionValueRequest request
    ) {
        return new ApiResponse<>(productOptionValueAdminService.updateValue(productOptionId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<@NonNull Void> deleteValue(
            @PathVariable("productOptionId") Long productOptionId,
            @PathVariable("id") Long id
    ) {
        productOptionValueAdminService.deleteValue(productOptionId, id);
        return new ApiResponse<>((Void) null);
    }
}
