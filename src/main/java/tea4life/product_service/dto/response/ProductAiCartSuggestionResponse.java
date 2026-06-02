package tea4life.product_service.dto.response;

import java.util.List;

public record ProductAiCartSuggestionResponse(
        String productId,
        String productName,
        String productImageUrl,
        Double unitPrice,
        Integer quantity,
        List<ProductAiCartOptionSelectionResponse> selectedOptions
) {
}
