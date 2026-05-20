package tea4life.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProductAiChatRequest(
        @NotBlank(message = "message khong duoc de trong")
        String message
) {
}
