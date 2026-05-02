package tea4life.product_service.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.product_service.model.ProductOption;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
}
