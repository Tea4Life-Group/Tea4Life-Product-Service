package tea4life.product_service.controller.admin.product;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.response.ProductSummaryResponse;
import tea4life.product_service.service.ProductFavoriteService;

/**
 * @author : user664dntp
 * @mailto : phatdang19052004@gmail.com
 * @created : 8/05/2026, Friday
 **/
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/products/favorites")
public class ProductFavoriteController {

    ProductFavoriteService productFavoriteService;

    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> getMyFavorites(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return new ApiResponse<>(productFavoriteService.getMyFavorites(pageable));
    }

    @PostMapping("/{productId}")
    public ApiResponse<String> addFavorite(
            @PathVariable("productId") Long productId
    ) {
        productFavoriteService.addFavorite(productId);
        return new ApiResponse<>("Đã thêm " + productId + " vào danh sách yêu thích");
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<String> removeFavorite(
            @PathVariable("productId") Long productId
    ) {
        productFavoriteService.removeFavorite(productId);
        return new ApiResponse<>("Đã xóa " + productId + " khỏi danh sách yêu thích");
    }
}
