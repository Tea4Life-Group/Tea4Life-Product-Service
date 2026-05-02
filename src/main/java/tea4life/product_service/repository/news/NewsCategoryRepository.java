package tea4life.product_service.repository.news;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.product_service.model.NewsCategory;

import java.util.Optional;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:28 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.repository.news
 */

public interface NewsCategoryRepository extends JpaRepository<NewsCategory, Long> {
    Optional<NewsCategory> findBySlug(String slug);
}
