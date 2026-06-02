package tea4life.product_service.dto.response;

public record ProductAiCartOptionSelectionResponse(
        String productOptionId,
        String productOptionName,
        String productOptionValueId,
        String productOptionValueName,
        Double extraPrice
) {
}
