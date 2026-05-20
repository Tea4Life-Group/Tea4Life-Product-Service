package tea4life.product_service.dto.response;

public record ProductAiChatConfigResponse(
        String chatboxDisplayName,
        Integer maxQuestionsPerUserPerDay
) {
}
