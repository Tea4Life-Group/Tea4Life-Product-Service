package tea4life.product_service.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import tea4life.product_service.config.database.SnowflakeGenerated;
import tea4life.product_service.model.base.BaseEntity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author Le Tran Gia Huy
 * @created 06/04/2026 - 5:42 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.model
 */

@Entity
@Table(name = "news_categories")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewsCategory extends BaseEntity {
    @Id
    @SnowflakeGenerated
    Long id;

    String name;
    @Column(unique = true)
    String slug;

    @OneToMany(mappedBy = "category")
    List<News> newsList = new ArrayList<>();

    @PrePersist
    public void generateSlug() {
        // Chỉ sinh slug nếu hiện tại nó đang trống
        if (this.name != null && (this.slug == null || this.slug.isEmpty())) {
            // 1. Xử lý chữ Đ/đ
            String customName = this.name.replace("Đ", "D").replace("đ", "d");
            // 2. Bỏ dấu tiếng Việt
            String normalized = Normalizer.normalize(customName, Normalizer.Form.NFD);
            Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
            String noAccent = pattern.matcher(normalized).replaceAll("");
            // 3. Làm sạch chuỗi và gán trực tiếp làm slug
            this.slug = noAccent.toLowerCase(Locale.ENGLISH)
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", "-");
        }
    }
}
