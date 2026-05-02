package tea4life.product_service.utils;

import org.springframework.stereotype.Component;
import tea4life.product_service.dto.response.NewsCategoryResponse;
import tea4life.product_service.dto.response.NewsChunkResponse;
import tea4life.product_service.dto.response.NewsDetailResponse;
import tea4life.product_service.dto.response.NewsSummaryResponse;
import tea4life.product_service.model.News;
import tea4life.product_service.model.NewsCategory;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 5:05 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.utils
 */

@Component
public class NewsMapper {
    public NewsDetailResponse mapToDetailResponse(News news) {
        return new NewsDetailResponse(
                news.getId().toString(),
                news.getTitle(),
                news.getSlug(),
                news.getThumbnailUrl(),
                mapToCategoryResponse(news.getCategory()),
                news.getChunks().stream()
                        .map(chunk -> new NewsChunkResponse(
                                // Chuyển Long ID sang String
                                chunk.getId().toString(),
                                chunk.getType(),
                                chunk.getContent(),
                                chunk.getSortIndex()
                        ))
                        .toList(),
                news.getCreatedAt(),
                news.getUpdatedAt()
        );
    }

    public NewsCategoryResponse mapToCategoryResponse(NewsCategory category) {
        return new NewsCategoryResponse(
                category.getId().toString(),
                category.getName(),
                category.getSlug()
        );
    }

    public NewsSummaryResponse mapToSummaryResponse(News news) {
        return new NewsSummaryResponse(
                news.getId().toString(),
                news.getTitle(),
                news.getSlug(),
                news.getThumbnailUrl(),
                news.getCategory().getName(),
                news.getCategory().getSlug(),
                news.getCreatedAt()
        );
    }
}
