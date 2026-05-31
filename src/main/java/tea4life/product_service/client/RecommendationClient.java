package tea4life.product_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.response.ProductPopularityResponse;
import tea4life.product_service.dto.response.RecommendedOptionValueResponse;
import tea4life.product_service.dto.response.RelatedProductResponse;

import java.util.List;

@FeignClient(name = "TEA4LIFE-RECOMMENDATION-SERVICE", url = "${service.url.recommendation}")
public interface RecommendationClient {

    @GetMapping("/internal/recommendations/products/popularity/top")
    ApiResponse<List<ProductPopularityResponse>> getPopularProducts(
            @RequestParam(name = "limit", required = false) Integer limit
    );

    @GetMapping("/internal/recommendations/products/{productId}/popularity")
    ApiResponse<ProductPopularityResponse> getProductPopularity(@PathVariable("productId") Long productId);

    @GetMapping("/internal/recommendations/products/popularity")
    ApiResponse<List<ProductPopularityResponse>> getProductPopularities(
            @RequestParam("productIds") List<Long> productIds
    );

    @GetMapping("/internal/recommendations/products/{productId}/related")
    ApiResponse<List<RelatedProductResponse>> getRelatedProducts(
            @PathVariable("productId") Long productId,
            @RequestParam(name = "limit", required = false) Integer limit
    );

    @GetMapping("/internal/recommendations/products/{productId}/option-values/recommended")
    ApiResponse<List<RecommendedOptionValueResponse>> getRecommendedOptionValues(
            @PathVariable("productId") Long productId,
            @RequestParam(name = "limit", required = false) Integer limit
    );
}
