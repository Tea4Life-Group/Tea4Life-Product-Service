package tea4life.product_service.service;

import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.ProductAiChatRequest;
import tea4life.product_service.dto.response.ProductAiChatResponse;
import tea4life.product_service.dto.response.ProductAiChatConfigResponse;
import tea4life.product_service.dto.response.ProductAiChatHistoryItemResponse;

public interface ProductAiChatService {
    ProductAiChatResponse chat(ProductAiChatRequest request);

    ProductAiChatConfigResponse getPublicConfig();

    PageResponse<ProductAiChatHistoryItemResponse> getHistory(int page, int size);
}
