package tea4life.product_service.product.service;

import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.response.PopularProductCardResponse;
import tea4life.product_service.dto.response.ProductDetailResponse;
import tea4life.product_service.dto.response.ProductSummaryResponse;
import tea4life.product_service.dto.response.RecommendedOptionValueResponse;

import java.util.List;

public interface ProductService {
    @Transactional(readOnly = true)
    PageResponse<ProductSummaryResponse> findProducts(
            Pageable pageable,
            String keyword,
            Long categoryId,
            Double minPrice,
            Double maxPrice
    );

    @Transactional(readOnly = true)
    ProductDetailResponse findProductById(Long id);

    @Transactional(readOnly = true)
    List<PopularProductCardResponse> getPopularProducts(Integer limit);

    @Transactional(readOnly = true)
    List<ProductSummaryResponse> getRelatedProducts(Long productId, Integer limit);

    @Transactional(readOnly = true)
    List<RecommendedOptionValueResponse> getRecommendedOptionValues(Long productId, Integer limit);

    @Transactional(readOnly = true)
    List<ProductSummaryResponse> getRandomProducts();
}

