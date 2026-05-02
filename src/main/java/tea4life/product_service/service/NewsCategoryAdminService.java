package tea4life.product_service.service;

import tea4life.product_service.dto.request.NewsCategoryRequest;
import tea4life.product_service.dto.response.NewsCategoryResponse;

import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 3:17 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.service
 */

public interface NewsCategoryAdminService {
    List<NewsCategoryResponse> findAllNewsCategory();
    NewsCategoryResponse findById(Long id);
    NewsCategoryResponse findBySlug(String slug);
    NewsCategoryResponse create(NewsCategoryRequest request);
    NewsCategoryResponse update(Long id, NewsCategoryRequest request);
    void delete(Long id);
}
