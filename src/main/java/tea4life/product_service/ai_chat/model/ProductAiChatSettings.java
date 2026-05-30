package tea4life.product_service.ai_chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import tea4life.product_service.model.base.BaseEntity;

@Entity
@Table(name = "product_ai_chat_settings")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductAiChatSettings extends BaseEntity {

    @Id
    Long id;

    @Column(nullable = false, length = 120)
    String chatboxDisplayName;

    @Column(nullable = false)
    Integer maxQuestionsPerUserPerDay;
}

