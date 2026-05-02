package tea4life.product_service.controller.admin.product;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.*;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.request.CreateProductOptionRequest;
import tea4life.product_service.dto.response.ProductOptionResponse;
import tea4life.product_service.service.ProductOptionAdminService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/product-options")
public class ProductOptionAdminController {

    ProductOptionAdminService productOptionAdminService;

    @PostMapping()
    public ApiResponse<ProductOptionResponse> createOption(
            @RequestBody @Valid CreateProductOptionRequest request
    ) {
        return new ApiResponse<>(productOptionAdminService.createOption(request));
    }

    @GetMapping()
    public ApiResponse<List<ProductOptionResponse>> findAllOptions() {
        return new ApiResponse<>(productOptionAdminService.findAllOptions());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductOptionResponse> findOptionById(
            @PathVariable("id") Long id
    ) {
        return new ApiResponse<>(productOptionAdminService.findOptionById(id));
    }

    @PostMapping("/{id}")
    public ApiResponse<ProductOptionResponse> updateOption(
            @PathVariable("id") Long id,
            @RequestBody @Valid CreateProductOptionRequest request
    ) {
        return new ApiResponse<>(productOptionAdminService.updateOption(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<@NonNull Void> deleteOption(
            @PathVariable("id") Long id
    ) {
        productOptionAdminService.deleteOption(id);
        return new ApiResponse<>((Void) null);
    }
}
