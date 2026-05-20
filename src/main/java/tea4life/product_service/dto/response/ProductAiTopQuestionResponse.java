package tea4life.product_service.dto.response;

public record ProductAiTopQuestionResponse(
        String question,
        Long questionCount
) {
}
