package tea4life.product_service.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.client.StorageClient;
import tea4life.product_service.context.UserContext;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.event.OptionValueAuditEvent;
import tea4life.product_service.dto.request.CreateProductOptionValueRequest;
import tea4life.product_service.dto.request.FileMoveRequest;
import tea4life.product_service.dto.response.ProductOptionValueResponse;
import tea4life.product_service.model.ProductOption;
import tea4life.product_service.model.ProductOptionValue;
import tea4life.product_service.model.enums.AuditAction;
import tea4life.product_service.repository.product.ProductOptionRepository;
import tea4life.product_service.repository.product.ProductOptionValueRepository;
import tea4life.product_service.service.ProductOptionValueAdminService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ProductOptionValueAdminServiceImpl implements ProductOptionValueAdminService {

    ProductOptionRepository productOptionRepository;
    ProductOptionValueRepository productOptionValueRepository;
    StorageClient storageClient;
    KafkaTemplate<String, String> kafkaTemplate;
    KafkaTemplate<String, Object> kafkaObjectTemplate;

    @NonFinal
    String storageDeleteFileTopic;

    @NonFinal
    String auditLogTopic;

    public ProductOptionValueAdminServiceImpl(
            ProductOptionRepository productOptionRepository,
            ProductOptionValueRepository productOptionValueRepository,
            StorageClient storageClient,
            @Qualifier("kafkaStringTemplate") KafkaTemplate<String, String> kafkaTemplate,
            @Qualifier("kafkaObjectTemplate") KafkaTemplate<String, Object> kafkaObjectTemplate,
            @Value("${spring.kafka.topic.storage-delete-file}") String storageDeleteFileTopic,
            @Value("${spring.kafka.topic.audit-log}") String auditLogTopic
    ) {
        this.productOptionRepository = productOptionRepository;
        this.productOptionValueRepository = productOptionValueRepository;
        this.storageClient = storageClient;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaObjectTemplate = kafkaObjectTemplate;
        this.storageDeleteFileTopic = storageDeleteFileTopic;
        this.auditLogTopic = auditLogTopic;
    }

    @Override
    public ProductOptionValueResponse createValue(Long productOptionId, CreateProductOptionValueRequest request) {
        Long targetProductOptionId = resolveTargetProductOptionId(productOptionId, request.productOptionId());
        ProductOption option = findOptionById(targetProductOptionId);

        ProductOptionValue value = new ProductOptionValue();
        value.setProductOption(option);
        applyRequestToValue(value, request);

        ProductOptionValue savedValue = productOptionValueRepository.save(value);
        confirmImageIfNeeded(savedValue, request.imageKey());

        ProductOptionValue finalSavedValue = productOptionValueRepository.save(savedValue);

        publishOptionValueAudit(finalSavedValue.getId(), finalSavedValue.getValueName(), option.getName(), AuditAction.CREATE);

        return toResponse(finalSavedValue);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductOptionValueResponse> findAllValues(Long productOptionId, int page, int size) {
        ensureOptionExists(productOptionId);

        int resolvedPage = Math.max(page, 1);
        int resolvedSize = Math.max(size, 1);

        Pageable pageable = PageRequest.of(
                resolvedPage - 1,
                resolvedSize,
                Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.ASC, "id"))
        );

        Page<@NonNull ProductOptionValueResponse> responsePage = productOptionValueRepository
                .findAllByProductOptionId(productOptionId, pageable)
                .map(this::toResponse);

        return new PageResponse<>(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductOptionValueResponse findValueById(Long productOptionId, Long id) {
        ensureOptionExists(productOptionId);
        return toResponse(findValueEntityById(productOptionId, id));
    }

    @Override
    public ProductOptionValueResponse updateValue(Long productOptionId, Long id, CreateProductOptionValueRequest request) {
        ensureOptionExists(productOptionId);
        ProductOptionValue value = productOptionValueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product option value not found"));

        Long targetProductOptionId = resolveTargetProductOptionId(productOptionId, request.productOptionId());
        if (!value.getProductOption().getId().equals(targetProductOptionId)) {
            value.setProductOption(findOptionById(targetProductOptionId));
        }

        String oldImageUrl = value.getImageUrl();
        applyRequestToValue(value, request);

        ProductOptionValue savedValue = productOptionValueRepository.save(value);
        confirmImageIfNeeded(savedValue, request.imageKey());

        ProductOptionValue refreshedValue = productOptionValueRepository.save(savedValue);
        if (hasText(request.imageKey()) && hasText(oldImageUrl) && !oldImageUrl.equals(refreshedValue.getImageUrl())) {
            publishStorageDelete(oldImageUrl);
        }

        publishOptionValueAudit(refreshedValue.getId(), refreshedValue.getValueName(), refreshedValue.getProductOption().getName(), AuditAction.UPDATE);

        return toResponse(refreshedValue);
    }

    @Override
    public void deleteValue(Long productOptionId, Long id) {
        ensureOptionExists(productOptionId);
        ProductOptionValue value = productOptionValueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product option value not found"));

        String imageUrl = value.getImageUrl();
        String valueName = value.getValueName();
        String optionName = value.getProductOption().getName();

        productOptionValueRepository.delete(value);
        publishStorageDelete(imageUrl);

        publishOptionValueAudit(id, valueName, optionName, AuditAction.DELETE);
    }

    private ProductOption findOptionById(Long productOptionId) {
        return productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new EntityNotFoundException("Product option not found"));
    }

    private void ensureOptionExists(Long productOptionId) {
        if (!productOptionRepository.existsById(productOptionId)) {
            throw new EntityNotFoundException("Product option not found");
        }
    }

    private ProductOptionValue findValueEntityById(Long productOptionId, Long id) {
        return productOptionValueRepository.findByIdAndProductOptionId(id, productOptionId)
                .orElseThrow(() -> new EntityNotFoundException("Product option value not found"));
    }

    private void applyRequestToValue(ProductOptionValue value, CreateProductOptionValueRequest request) {
        value.setValueName(request.valueName());
        value.setExtraPrice(request.extraPrice());
        value.setSortOrder(request.sortOrder());
    }

    private void confirmImageIfNeeded(ProductOptionValue value, String imageKey) {
        if (!hasText(imageKey) || value.getId() == null) {
            return;
        }

        String destinationPath = "products/options/values/" + value.getId();
        ApiResponse<String> storageResponse = storageClient.confirmFile(
                new FileMoveRequest(imageKey, destinationPath)
        );
        value.setImageUrl(storageResponse.getData());
    }

    private Long resolveTargetProductOptionId(Long fallbackProductOptionId, String requestedProductOptionId) {
        if (!hasText(requestedProductOptionId)) {
            return fallbackProductOptionId;
        }

        try {
            return Long.parseLong(requestedProductOptionId.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid productOptionId", ex);
        }
    }

    private void publishStorageDelete(String fileUrl) {
        if (hasText(fileUrl)) {
            kafkaTemplate.send(storageDeleteFileTopic, fileUrl);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ProductOptionValueResponse toResponse(ProductOptionValue value) {
        return new ProductOptionValueResponse(
                value.getId() == null ? null : value.getId().toString(),
                value.getProductOption() == null ? null : value.getProductOption().getId().toString(),
                value.getValueName(),
                value.getExtraPrice(),
                value.getSortOrder(),
                value.getImageUrl()
        );
    }

    private void publishOptionValueAudit(Long valueId, String valueName, String parentOptionName, AuditAction action) {
        if (valueId == null) return;

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
                case CREATE -> "THÊM MỚI GIÁ TRỊ TÙY CHỌN";
                case UPDATE -> "CẬP NHẬT GIÁ TRỊ TÙY CHỌN";
                case DELETE -> "XÓA GIÁ TRỊ TÙY CHỌN";
            };

            String message = String.format("[%s] %s đã thực hiện hành động %s: %s (Thuộc tùy chọn: %s)",
                    timeString, performerEmail, actionVn, valueName, parentOptionName);

            OptionValueAuditEvent event = new OptionValueAuditEvent(
                    valueId,
                    valueName,
                    action,
                    performerEmail,
                    currentTime,
                    message
            );

            kafkaObjectTemplate.send(auditLogTopic, event);
            log.info("Đã bắn audit event {} cho option value id={}", action.name(), valueId);

        } catch (Exception ex) {
            log.warn("Failed to publish audit event cho optionValueId={}: {}", valueId, ex.getMessage());
        }
    }
}