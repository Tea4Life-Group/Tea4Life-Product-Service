package tea4life.product_service.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:11 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.dto.response
 */

public record NewsSummaryResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        String id,
        String title,
        String slug,
        String thumbnailUrl,
        String categoryName,
        String categorySlug,
        Instant createdAt
) {
}
