package tea4life.product_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import tea4life.product_service.config.database.SnowflakeGenerated;
import tea4life.product_service.model.base.BaseEntity;

@Entity
@Table(
        name = "product_ai_chat_messages",
        indexes = {
                @Index(name = "idx_ai_chat_user_created", columnList = "userKeycloakId, created_at"),
                @Index(name = "idx_ai_chat_normalized_question", columnList = "normalizedQuestion"),
                @Index(name = "idx_ai_chat_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductAiChatMessage extends BaseEntity {

    @Id
    @SnowflakeGenerated
    Long id;

    @Column(length = 120)
    String userKeycloakId;

    @Column(length = 320)
    String userEmail;

    @Column(length = 2000, nullable = false)
    String question;

    @Column(length = 512)
    String normalizedQuestion;

    @Column(length = 8000)
    String answer;

    @Column(nullable = false)
    boolean limitReached = false;
}
