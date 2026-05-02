package tea4life.product_service.controller.admin.product;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.CreateProductRequest;
import tea4life.product_service.dto.response.ProductResponse;
import tea4life.product_service.service.ProductAdminService;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/products")
public class ProductAdminController {

    ProductAdminService productAdminService;

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(
            @RequestBody @Valid CreateProductRequest request
    ) {
        return new ApiResponse<>(productAdminService.createProduct(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> findAllProducts(
            @PageableDefault Pageable pageable
    ) {
        return new ApiResponse<>(productAdminService.findAllProducts(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> findProductById(
            @PathVariable("id") Long id
    ) {
        return new ApiResponse<>(productAdminService.findProductById(id));
    }

    @PostMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable("id") Long id,
            @RequestBody @Valid CreateProductRequest request
    ) {
        return new ApiResponse<>(productAdminService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<@NonNull Void> deleteProduct(
            @PathVariable("id") Long id
    ) {
        productAdminService.deleteProduct(id);
        return new ApiResponse<>((Void) null);
    }
}
