package tea4life.product_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.response.ProductRatingStatsResponse;

import java.util.List;

@FeignClient(name = "TEA4LIFE-BLOG-SERVICE", url = "${service.url.blog}")
public interface BlogReviewClient {

    @GetMapping("/public/blog-reviews/product-ratings")
    ApiResponse<List<ProductRatingStatsResponse>> getProductRatingStats(
            @RequestParam("productIds") List<Long> productIds
    );
}
