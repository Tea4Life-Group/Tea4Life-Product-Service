package tea4life.product_service.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:15 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.dto.response
 */
public record NewsCategoryResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        String id,
        String name,
        String slug
) {
}
