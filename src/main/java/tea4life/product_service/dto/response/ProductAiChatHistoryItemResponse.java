package tea4life.product_service.dto.response;

import java.time.Instant;
import java.util.List;

public record ProductAiChatHistoryItemResponse(
        String question,
        String answer,
        List<ProductSummaryResponse> recommendedProducts,
        Instant askedAt
) {
}
