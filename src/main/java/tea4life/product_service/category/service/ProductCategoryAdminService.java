package tea4life.product_service.category.service;

import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.dto.request.CreateProductCategoryRequest;
import tea4life.product_service.dto.response.ProductCategoryResponse;

import java.util.List;

/**
 * Admin 2/25/2026
 *
 **/
public interface ProductCategoryAdminService {
    ProductCategoryResponse createCategory(CreateProductCategoryRequest request);

    @Transactional(readOnly = true)
    List<ProductCategoryResponse> findAllCategories();

    @Transactional(readOnly = true)
    ProductCategoryResponse findCategoryById(Long id);

    ProductCategoryResponse updateCategory(Long id, CreateProductCategoryRequest request);

    void deleteCategory(Long id);
}

