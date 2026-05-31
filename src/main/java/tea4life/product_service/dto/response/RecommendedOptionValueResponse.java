package tea4life.product_service.dto.response;

import java.time.Instant;

public record RecommendedOptionValueResponse(
        Long optionValueId,
        Double score,
        Instant lastUpdated
) {
}
