package tea4life.product_service.service;

import tea4life.product_service.dto.response.NewsCategoryResponse;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 10:03 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.service
 */
public interface NewsCategoryService {
    List<NewsCategoryResponse> findAllNewsCategory();
    NewsCategoryResponse findBySlug(String slug);
}
