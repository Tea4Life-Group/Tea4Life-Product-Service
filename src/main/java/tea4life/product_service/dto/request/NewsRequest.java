package tea4life.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:19 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.dto.request
 */

public record NewsRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        String title,
        String thumbnailUrl,
        @NotNull(message = "Danh mục không được để trống")
        Long categoryId,
        @NotEmpty(message = "Bài viết phải có ít nhất 1 nội dung")
        List<NewsChunkRequest> chunks
) {
}
