package tea4life.product_service.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.product_service.category.model.ProductCategory;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
}



