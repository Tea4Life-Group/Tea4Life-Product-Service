package tea4life.product_service.repository.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tea4life.product_service.model.News;

import java.util.List;
import java.util.Optional;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:07 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.repository.news
 */

public interface NewsRepository extends JpaRepository<News, Long> {
    @EntityGraph(attributePaths = {"category"})
    Optional<News> findBySlug(String slug);

    @EntityGraph(attributePaths = {"category"})
    List<News> findTop3ByOrderByCreatedAtDesc();

    @Query(value = "SELECT n FROM News n JOIN FETCH n.category",
            countQuery = "SELECT count(n) FROM News n")
    Page<News> findAllNewsWithCategory(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<News> findAllByCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<News> findAllByCategorySlug(String categorySlug, Pageable pageable);
}
