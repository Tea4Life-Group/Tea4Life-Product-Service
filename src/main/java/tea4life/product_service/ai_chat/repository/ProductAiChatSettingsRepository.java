package tea4life.product_service.ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.product_service.ai_chat.model.ProductAiChatSettings;

public interface ProductAiChatSettingsRepository extends JpaRepository<ProductAiChatSettings, Long> {
}



