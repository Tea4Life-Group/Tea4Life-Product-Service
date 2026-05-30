package tea4life.product_service.option.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.product_service.option.model.ProductOption;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
}



