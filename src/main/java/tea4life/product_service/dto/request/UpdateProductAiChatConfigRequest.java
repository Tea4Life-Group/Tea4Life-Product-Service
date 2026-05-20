package tea4life.product_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProductAiChatConfigRequest(
        @NotBlank(message = "chatboxDisplayName khong duoc de trong")
        String chatboxDisplayName,

        @NotNull(message = "maxQuestionsPerUserPerDay khong duoc de trong")
        @Min(value = 0, message = "maxQuestionsPerUserPerDay phai >= 0")
        Integer maxQuestionsPerUserPerDay
) {
}
