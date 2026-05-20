package tea4life.product_service.dto.response;

import java.time.Instant;

public record ProductAiUserUsageResponse(
        String userKeycloakId,
        String userEmail,
        Long questionCount,
        Long todayQuestionCount,
        Integer remainingQuestionsToday,
        Instant lastAskedAt
) {
}
