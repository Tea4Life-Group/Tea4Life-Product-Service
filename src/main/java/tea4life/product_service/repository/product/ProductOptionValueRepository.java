package tea4life.product_service.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.product_service.model.ProductOptionValue;

import java.util.Optional;

public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValue, Long> {
    Page<ProductOptionValue> findAllByProductOptionId(Long productOptionId, Pageable pageable);

    void deleteAllByProductOptionId(Long productOptionId);

    Optional<ProductOptionValue> findByIdAndProductOptionId(Long id, Long productOptionId);
}
