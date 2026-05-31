package tea4life.product_service.option.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tea4life.product_service.config.database.SnowflakeGenerated;
import tea4life.product_service.model.base.BaseEntity;

/**
 * Admin 2/26/2026
 *
 **/
@Entity
@Table(name = "product_option_values")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductOptionValue extends BaseEntity {

    @Id
    @SnowflakeGenerated
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    ProductOption productOption;

    @Column(nullable = false)
    String valueName;

    @Column(nullable = false)
    Double extraPrice = 0.0;

    @Column(name = "sort_order", nullable = false)
    Integer sortOrder = 0;

    @Column
    String imageUrl;

}

