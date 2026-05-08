package tea4life.product_service.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.product_service.model.ProductFavorite;

import java.util.Optional;

/**
 * @author : user664dntp
 * @mailto : phatdang19052004@gmail.com
 * @created : 8/05/2026, Friday
 **/
public interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, Long> {

    Page<ProductFavorite> findByUserKeycloakIdOrderByCreatedAtDesc(String userKeycloakId, Pageable pageable);

    boolean existsByUserKeycloakIdAndProductId(String userKeycloakId, Long productId);

    Optional<ProductFavorite> findByUserKeycloakIdAndProductId(String userKeycloakId, Long productId);
}
