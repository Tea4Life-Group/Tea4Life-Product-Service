package tea4life.product_service.option.service;

import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.dto.request.CreateProductOptionRequest;
import tea4life.product_service.dto.response.ProductOptionResponse;

import java.util.List;

public interface ProductOptionAdminService {
    ProductOptionResponse createOption(CreateProductOptionRequest request);

    @Transactional(readOnly = true)
    List<ProductOptionResponse> findAllOptions();

    @Transactional(readOnly = true)
    ProductOptionResponse findOptionById(Long id);

    ProductOptionResponse updateOption(Long id, CreateProductOptionRequest request);

    void deleteOption(Long id);
}

