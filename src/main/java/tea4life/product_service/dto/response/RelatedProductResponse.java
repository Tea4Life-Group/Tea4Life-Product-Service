package tea4life.product_service.dto.response;

import java.time.Instant;

public record RelatedProductResponse(
        Long productId,
        Double score,
        Instant lastUpdated
) {
}
