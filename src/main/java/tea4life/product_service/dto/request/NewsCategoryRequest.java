package tea4life.product_service.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 4:07 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.dto.request
 */
public record NewsCategoryRequest(
        @NotBlank(message = "Tên thể loại không được để trống")
        String name
) {
}
