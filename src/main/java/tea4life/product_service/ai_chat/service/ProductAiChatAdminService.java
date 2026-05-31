package tea4life.product_service.ai_chat.service;

import org.springframework.data.domain.Pageable;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.UpdateProductAiChatConfigRequest;
import tea4life.product_service.dto.response.ProductAiChatConfigResponse;
import tea4life.product_service.dto.response.ProductAiMonthlyQuestionResponse;
import tea4life.product_service.dto.response.ProductAiChatOverviewResponse;
import tea4life.product_service.dto.response.ProductAiTopQuestionResponse;
import tea4life.product_service.dto.response.ProductAiUserUsageResponse;

import java.util.List;
import java.time.Instant;

public interface ProductAiChatAdminService {
    ProductAiChatConfigResponse getConfig();

    ProductAiChatConfigResponse updateConfig(UpdateProductAiChatConfigRequest request);

    ProductAiChatOverviewResponse getOverview();

    List<ProductAiMonthlyQuestionResponse> getMonthlyQuestionStats();

    List<ProductAiTopQuestionResponse> getTopQuestions(Integer limit);

    PageResponse<ProductAiUserUsageResponse> getUserUsage(
            Pageable pageable,
            String emailKeyword,
            Instant fromTime,
            Instant toTime
    );
}

