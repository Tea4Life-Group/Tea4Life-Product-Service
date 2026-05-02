package tea4life.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tea4life.product_service.model.enums.NewsContentType;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:23 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.dto.request
 */
public record NewsChunkRequest(
        @NotNull(message = "Loại nội dung không được để trống")
        NewsContentType type,
        @NotBlank(message = "Nội dung không được để trống")
        String content,
        int sortIndex
) {
}
