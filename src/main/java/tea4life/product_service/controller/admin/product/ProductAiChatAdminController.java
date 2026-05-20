package tea4life.product_service.controller.admin.product;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.UpdateProductAiChatConfigRequest;
import tea4life.product_service.dto.response.ProductAiChatConfigResponse;
import tea4life.product_service.dto.response.ProductAiMonthlyQuestionResponse;
import tea4life.product_service.dto.response.ProductAiChatOverviewResponse;
import tea4life.product_service.dto.response.ProductAiTopQuestionResponse;
import tea4life.product_service.dto.response.ProductAiUserUsageResponse;
import tea4life.product_service.service.ProductAiChatAdminService;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/products/ai-chat")
public class ProductAiChatAdminController {

    ProductAiChatAdminService productAiChatAdminService;

    @GetMapping("/config")
    public ApiResponse<ProductAiChatConfigResponse> getConfig() {
        return new ApiResponse<>(productAiChatAdminService.getConfig());
    }

    @PostMapping("/config")
    public ApiResponse<ProductAiChatConfigResponse> updateConfig(
            @Valid @RequestBody UpdateProductAiChatConfigRequest request
    ) {
        return new ApiResponse<>(productAiChatAdminService.updateConfig(request));
    }

    @GetMapping("/stats/overview")
    public ApiResponse<ProductAiChatOverviewResponse> getOverview() {
        return new ApiResponse<>(productAiChatAdminService.getOverview());
    }

    @GetMapping("/stats/monthly")
    public ApiResponse<List<ProductAiMonthlyQuestionResponse>> getMonthlyQuestionStats() {
        return new ApiResponse<>(productAiChatAdminService.getMonthlyQuestionStats());
    }

    @GetMapping("/stats/top-questions")
    public ApiResponse<List<ProductAiTopQuestionResponse>> getTopQuestions(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit
    ) {
        return new ApiResponse<>(productAiChatAdminService.getTopQuestions(limit));
    }

    @GetMapping("/stats/users")
    public ApiResponse<PageResponse<ProductAiUserUsageResponse>> getUserUsage(
            @PageableDefault Pageable pageable,
            @RequestParam(value = "emailKeyword", required = false) String emailKeyword,
            @RequestParam(value = "fromTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromTime,
            @RequestParam(value = "toTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toTime
    ) {
        return new ApiResponse<>(productAiChatAdminService.getUserUsage(pageable, emailKeyword, fromTime, toTime));
    }
}
