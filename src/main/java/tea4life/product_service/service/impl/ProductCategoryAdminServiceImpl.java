package tea4life.product_service.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.client.StorageClient;
import tea4life.product_service.context.UserContext;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.event.CategoryAuditEvent;
import tea4life.product_service.dto.request.CreateProductCategoryRequest;
import tea4life.product_service.dto.request.FileMoveRequest;
import tea4life.product_service.dto.response.ProductCategoryResponse;
import tea4life.product_service.model.ProductCategory;
import tea4life.product_service.model.enums.AuditAction;
import tea4life.product_service.repository.product.ProductCategoryRepository;
import tea4life.product_service.service.ProductCategoryAdminService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin 2/25/2026
 *
 **/
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ProductCategoryAdminServiceImpl implements ProductCategoryAdminService {

    // Repository
    ProductCategoryRepository productCategoryRepository;

    // Client
    StorageClient storageClient;

    // Kafka
    KafkaTemplate<String, String> kafkaStringTemplate;
    KafkaTemplate<String, Object> kafkaObjectTemplate;

    @NonFinal
    String storageDeleteFileTopic;

    @NonFinal
    String auditLogTopic;

    public ProductCategoryAdminServiceImpl(
            ProductCategoryRepository productCategoryRepository, StorageClient storageClient,
            @Qualifier("kafkaStringTemplate") KafkaTemplate<String, String> kafkaStringTemplate,
            @Qualifier("kafkaObjectTemplate") KafkaTemplate<String, Object> kafkaObjectTemplate,
            @Value("${spring.kafka.topic.storage-delete-file}") String storageDeleteFileTopic,
            @Value("${spring.kafka.topic.audit-log}") String auditLogTopic
    ) {
        this.productCategoryRepository = productCategoryRepository;
        this.storageClient = storageClient;
        this.kafkaStringTemplate = kafkaStringTemplate;
        this.kafkaObjectTemplate = kafkaObjectTemplate;
        this.storageDeleteFileTopic = storageDeleteFileTopic;
        this.auditLogTopic = auditLogTopic;
    }

    @Override
    public ProductCategoryResponse createCategory(CreateProductCategoryRequest request) {
        if (productCategoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DataIntegrityViolationException("Tˆn danh m?c da t?n t?i");
        }

        ProductCategory category = new ProductCategory();
        applyRequestToCategory(category, request);
        category = productCategoryRepository.save(category);

        if (hasText(request.iconKey())) {
            String destinationPath = "products/categories/" + category.getId();
            ApiResponse<String> storageResponse = storageClient.confirmFile(
                    new FileMoveRequest(request.iconKey(), destinationPath)
            );
            if (storageResponse.getErrorCode() != null) {
                throw new RuntimeException("L?i di chuy?n file: " + storageResponse.getErrorMessage());
            }
            category.setIconUrl(storageResponse.getData());
            category = productCategoryRepository.save(category);
        }

        publishCategoryAudit(category.getId(), category.getName(), AuditAction.CREATE);
        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> findAllCategories() {
        return productCategoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponse findCategoryById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    public ProductCategoryResponse updateCategory(Long id, CreateProductCategoryRequest request) {
        ProductCategory category = findById(id);

        if (!category.getName().equalsIgnoreCase(request.name())
                && productCategoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DataIntegrityViolationException("Tˆn danh m?c da t?n t?i");
        }

        applyRequestToCategory(category, request);
        if (hasText(request.iconKey())) {
            String oldIconUrl = category.getIconUrl();
            String destinationPath = "products/categories/" + category.getId();
            ApiResponse<String> storageResponse = storageClient.confirmFile(
                    new FileMoveRequest(request.iconKey(), destinationPath)
            );
            if (storageResponse.getErrorCode() != null) {
                throw new RuntimeException("L?i di chuy?n file: " + storageResponse.getErrorMessage());
            }
            category.setIconUrl(storageResponse.getData());
            publishStorageDelete(oldIconUrl);
        }

        ProductCategory saved = productCategoryRepository.save(category);
        publishCategoryAudit(saved.getId(), saved.getName(), AuditAction.UPDATE);
        return toResponse(saved);
    }

    @Override
    public void deleteCategory(Long id) {
        ProductCategory category = findById(id);
        String iconUrl = category.getIconUrl();
        productCategoryRepository.delete(category);
        publishStorageDelete(iconUrl);
        publishCategoryAudit(id, category.getName(), AuditAction.DELETE);
    }

    private ProductCategory findById(Long id) {
        return productCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Kh“ng tm th?y danh m?c s?n ph?m"));
    }

    private void applyRequestToCategory(ProductCategory category, CreateProductCategoryRequest request) {
        category.setName(request.name());
        category.setDescription(request.description());
    }

    private ProductCategoryResponse toResponse(ProductCategory category) {
        return ProductCategoryResponse.builder()
                .id(category.getId() == null ? null : category.getId().toString())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void publishStorageDelete(String fileUrl) {
        if (hasText(fileUrl)) {
            kafkaStringTemplate.send(storageDeleteFileTopic, fileUrl);
        }
    }

    private void publishCategoryAudit(Long categoryId, String categoryName, AuditAction action) {
        if (categoryId == null) return;

        try {
            UserContext context = UserContext.get();
            String performerEmail = "system@tea4life.com";

            if (context != null && context.getEmail() != null && !context.getEmail().isBlank()) {
                performerEmail = context.getEmail();
            }

            long currentTime = System.currentTimeMillis();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
                    .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
            String timeString = formatter.format(Instant.ofEpochMilli(currentTime));

            String actionVn = switch (action) {
                case CREATE -> "THÊM MỚI DANH MỤC";
                case UPDATE -> "CẬP NHẬT DANH MỤC";
                case DELETE -> "XÓA DANH MỤC";
            };

            String message = String.format("[%s] %s đã thực hiện hành động %s: %s",
                    timeString, performerEmail, actionVn, categoryName);

            CategoryAuditEvent event = new CategoryAuditEvent(
                    categoryId,
                    categoryName,
                    action,
                    performerEmail,
                    currentTime,
                    message
            );

            kafkaObjectTemplate.send(auditLogTopic, event);
            log.info("Đã bắn audit event {} cho danh mục id={}", action.name(), categoryId);

        } catch (Exception ex) {
            log.warn("Failed to publish audit event cho categoryId={}: {}", categoryId, ex.getMessage());
        }
    }
}



