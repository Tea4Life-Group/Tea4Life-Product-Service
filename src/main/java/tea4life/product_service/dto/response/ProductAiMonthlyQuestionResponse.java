package tea4life.product_service.dto.response;

public record ProductAiMonthlyQuestionResponse(
        Integer month,
        Long questionCount
) {
}
