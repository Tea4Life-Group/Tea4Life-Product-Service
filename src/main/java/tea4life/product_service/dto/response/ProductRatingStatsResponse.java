package tea4life.product_service.dto.response;

public record ProductRatingStatsResponse(
        String productId,
        long reviewCount,
        double averageRating
) {
}
