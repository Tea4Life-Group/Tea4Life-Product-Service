package tea4life.product_service.dto.response;

import java.util.List;

public record ProductAiChatResponse(
        String answer,
        List<ProductSummaryResponse> recommendedProducts,
        String chatboxDisplayName,
        Integer maxQuestionsPerUserPerDay,
        Integer remainingQuestionsToday,
        Boolean limitReached
) {
}
