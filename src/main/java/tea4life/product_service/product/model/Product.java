package tea4life.product_service.product.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tea4life.product_service.category.model.ProductCategory;
import tea4life.product_service.config.database.SnowflakeGenerated;
import tea4life.product_service.model.base.BaseEntity;
import tea4life.product_service.option.model.ProductOption;

import java.util.List;

/**
 * Admin 2/26/2026
 *
 **/
@Entity
@Table(name = "products")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product extends BaseEntity {

    @Id
    @SnowflakeGenerated
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_category_id", nullable = false)
    ProductCategory productCategory;

    String name;
    String description;
    Double basePrice;
    String imageUrl;

    @ManyToMany
    @JoinTable(
            name = "product_option_mapping",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "product_option_id")
    )
    List<ProductOption> productOptions;

}

