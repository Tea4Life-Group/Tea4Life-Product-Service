package tea4life.product_service.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.product_service.model.ProductCategory;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
}
