package tea4life.product_service.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tea4life.product_service.config.database.SnowflakeGenerated;
import tea4life.product_service.model.base.BaseEntity;
import tea4life.product_service.model.enums.NewsContentType;

/**
 * @author Le Tran Gia Huy
 * @created 06/04/2026 - 5:53 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.model
 */

@Entity
@Table(name = "news_chunks")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewsChunk extends BaseEntity {
    @Id
    @SnowflakeGenerated
    Long id;

    @Enumerated(EnumType.STRING)
    private NewsContentType type; // TEXT, IMAGE, PRODUCT
    @Column(columnDefinition = "TEXT")
    String content;
    @Column(name = "sort_index")
    int sortIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id")
    News news;
}
