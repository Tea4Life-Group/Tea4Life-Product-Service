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
@Table(name = "news")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class News extends BaseEntity {
    @Id
    @SnowflakeGenerated
    Long id;

    String title;
    @Column(unique = true)
    String slug;
    @Column(name = "thumbnail_url")
    String thumbnailUrl;

    @OneToMany(mappedBy = "news", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortIndex ASC") // Tự động sắp xếp khi fetch
    private List<NewsChunk> chunks = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private NewsCategory category;

    public void addChunk(NewsChunk chunk) {
        chunks.add(chunk);
        chunk.setNews(this);
    }

    @PrePersist
    public void generateSlug() {
        // Chỉ sinh slug nếu hiện tại nó đang trống
        if (this.title != null && (this.slug == null || this.slug.isEmpty())) {
            // 1. Xử lý chữ Đ/đ
            String customTitle = this.title.replace("Đ", "D").replace("đ", "d");
            // 2. Bỏ dấu tiếng Việt
            String normalized = Normalizer.normalize(customTitle, Normalizer.Form.NFD);
            Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
            String noAccent = pattern.matcher(normalized).replaceAll("");
            // 3. Làm sạch chuỗi
            String baseSlug = noAccent.toLowerCase(Locale.ENGLISH)
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", "-");
            // 4. Sinh một chuỗi random 5 ký tự từ UUID
            String randomString = UUID.randomUUID().toString().substring(0, 5);
            // 5. Nối lại thành slug hoàn chỉnh
            this.slug = baseSlug + "-" + randomString;
        }
    }
}
