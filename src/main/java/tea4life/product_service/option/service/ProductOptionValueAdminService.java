package tea4life.product_service.option.service;

import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.CreateProductOptionValueRequest;
import tea4life.product_service.dto.response.ProductOptionValueResponse;

public interface ProductOptionValueAdminService {
    ProductOptionValueResponse createValue(Long productOptionId, CreateProductOptionValueRequest request);

    @Transactional(readOnly = true)
    PageResponse<ProductOptionValueResponse> findAllValues(Long productOptionId, int page, int size);

    @Transactional(readOnly = true)
    ProductOptionValueResponse findValueById(Long productOptionId, Long id);

    ProductOptionValueResponse updateValue(Long productOptionId, Long id, CreateProductOptionValueRequest request);

    void deleteValue(Long productOptionId, Long id);
}

