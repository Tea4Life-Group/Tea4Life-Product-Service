package tea4life.product_service.dto.response;

public record ProductAiChatOverviewResponse(
        Long totalQuestions,
        Long uniqueUsers,
        Long questionsToday,
        Integer maxQuestionsPerUserPerDay
) {
}
