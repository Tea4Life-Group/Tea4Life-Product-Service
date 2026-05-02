package tea4life.product_service.repository.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tea4life.product_service.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"productCategory", "productOptions"})
    Page<Product> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"productCategory", "productOptions"})
    @Query("""
            select p
            from Product p
            where (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
              and (:categoryId is null or p.productCategory.id = :categoryId)
              and (:minPrice is null or p.basePrice >= :minPrice)
              and (:maxPrice is null or p.basePrice <= :maxPrice)
              and p.active = true
            """)
    Page<Product> findAllByFilters(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"productCategory", "productOptions"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"productCategory", "productOptions"})
    Optional<Product> findDetailById(Long id);

    @EntityGraph(attributePaths = {"productCategory"})
    List<Product> findByIdInAndActiveTrue(List<Long> ids);

    @EntityGraph(attributePaths = {"productCategory"})
    Page<Product> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = """
            select *
            from products
            where active = true
            order by rand()
            limit :limit
            """, nativeQuery = true)
    List<Product> findRandomActiveProducts(@Param("limit") int limit);
}
