package tea4life.product_service.favorite.service;

import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.response.ProductSummaryResponse;

/**
 * @author : user664dntp
 * @mailto : phatdang19052004@gmail.com
 * @created : 8/05/2026, Friday
 **/
public interface ProductFavoriteService {
    void addFavorite(Long productId);

    void removeFavorite(Long productId);

    @Transactional(readOnly = true)
    PageResponse<ProductSummaryResponse> getMyFavorites(Pageable pageable);
}

