package tea4life.product_service.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import tea4life.product_service.model.ProductAiChatSettings;

public interface ProductAiChatSettingsRepository extends JpaRepository<ProductAiChatSettings, Long> {
}
