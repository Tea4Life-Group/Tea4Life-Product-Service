package tea4life.product_service.product.policy;

import org.springframework.stereotype.Component;

@Component
public class ProductQueryPolicy {

    public void validatePriceRange(Double minPrice, Double maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice must be less than or equal to maxPrice");
        }
    }
}
