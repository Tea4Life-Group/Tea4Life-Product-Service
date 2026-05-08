package tea4life.product_service.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tea4life.product_service.config.database.SnowflakeGenerated;
import tea4life.product_service.model.base.BaseEntity;

/**
 * @author : user664dntp
 * @mailto : phatdang19052004@gmail.com
 * @created : 8/05/2026, Friday
 **/

@Entity
@Table(name = "product_favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_keycloak_id", "product_id"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductFavorite extends BaseEntity {
    @Id
    @SnowflakeGenerated
    Long id;

    @Column(name = "user_keycloak_id", nullable = false)
    String userKeycloakId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;
}