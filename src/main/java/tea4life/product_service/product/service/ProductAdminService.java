package tea4life.product_service.product.service;

import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.CreateProductRequest;
import tea4life.product_service.dto.response.ProductResponse;

public interface ProductAdminService {

    ProductResponse createProduct(CreateProductRequest request);

    @Transactional(readOnly = true)
    PageResponse<ProductResponse> findAllProducts(Pageable pageable);

    @Transactional(readOnly = true)
    ProductResponse findProductById(Long id);

    ProductResponse updateProduct(Long id, CreateProductRequest request);

    void deleteProduct(Long id);
}

