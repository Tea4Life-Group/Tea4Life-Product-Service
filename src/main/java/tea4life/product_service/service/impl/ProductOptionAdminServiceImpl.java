package tea4life.product_service.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.context.UserContext;
import tea4life.product_service.dto.event.OptionAuditEvent;
import tea4life.product_service.dto.request.CreateProductOptionRequest;
import tea4life.product_service.dto.request.CreateProductOptionValueRequest;
import tea4life.product_service.dto.response.ProductOptionResponse;
import tea4life.product_service.dto.response.ProductOptionValueResponse;
import tea4life.product_service.model.ProductOption;
import tea4life.product_service.model.ProductOptionValue;
import tea4life.product_service.model.enums.AuditAction;
import tea4life.product_service.repository.product.ProductOptionRepository;
import tea4life.product_service.repository.product.ProductOptionValueRepository;
import tea4life.product_service.service.ProductOptionAdminService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Slf4j
public class ProductOptionAdminServiceImpl implements ProductOptionAdminService {

    ProductOptionRepository productOptionRepository;
    ProductOptionValueRepository productOptionValueRepository;
    KafkaTemplate<String, Object> kafkaObjectTemplate;

    @NonFinal
    @Value("${spring.kafka.topic.audit-log}")
    String auditLogTopic;

    @Override
    public ProductOptionResponse createOption(CreateProductOptionRequest request) {
        ProductOption option = new ProductOption();
        applyRequestToOption(option, request);
        option = productOptionRepository.save(option);
        publishOptionAudit(option.getId(), option.getName(), request.productOptionValues(), AuditAction.CREATE);
        return toResponse(option);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductOptionResponse> findAllOptions() {
        return productOptionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductOptionResponse findOptionById(Long id) {
        return toResponse(findOptionByIdInternal(id));
    }

    @Override
    public ProductOptionResponse updateOption(Long id, CreateProductOptionRequest request) {
        ProductOption option = findOptionByIdInternal(id);
        applyRequestToOption(option, request);
        option = productOptionRepository.save(option);
        publishOptionAudit(option.getId(), option.getName(), request.productOptionValues(), AuditAction.UPDATE);
        return toResponse(option);
    }

    @Override
    public void deleteOption(Long id) {
        ProductOption option = findOptionByIdInternal(id);
        productOptionRepository.delete(option);
        publishOptionAudit(option.getId(), option.getName(), null, AuditAction.DELETE);
    }

    private ProductOption findOptionByIdInternal(Long id) {
        return productOptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy product option"));
    }

    private void applyRequestToOption(ProductOption option, CreateProductOptionRequest request) {
        option.setName(request.name());
        option.setRequired(request.isRequired());
        option.setMultiSelect(request.isMultiSelect());
        option.setSortOrder(request.sortOrder());
        if (option.getId() != null) {
            productOptionValueRepository.deleteAllByProductOptionId(option.getId());
        }
        option.setProductOptionValues(buildProductOptionValues(option, request.productOptionValues()));
    }

    private ProductOptionResponse toResponse(ProductOption option) {
        List<ProductOptionValueResponse> productOptionValues = option.getProductOptionValues() == null
                ? List.of()
                : option.getProductOptionValues().stream()
                .map(this::toValueResponse)
                .toList();

        return new ProductOptionResponse(
                option.getId() == null ? null : option.getId().toString(),
                option.getName(),
                option.isRequired(),
                option.isMultiSelect(),
                option.getSortOrder(),
                productOptionValues
        );
    }

    private List<ProductOptionValue> buildProductOptionValues(
            ProductOption option,
            List<CreateProductOptionValueRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }

        return requests.stream()
                .map(request -> {
                    ProductOptionValue value = new ProductOptionValue();
                    value.setProductOption(option);
                    value.setValueName(request.valueName());
                    value.setExtraPrice(request.extraPrice());
                    value.setSortOrder(request.sortOrder());
                    return value;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ProductOptionValueResponse toValueResponse(ProductOptionValue value) {
        return new ProductOptionValueResponse(
                value.getId() == null ? null : value.getId().toString(),
                value.getProductOption() == null ? null : value.getProductOption().getId().toString(),
                value.getValueName(),
                value.getExtraPrice(),
                value.getSortOrder(),
                value.getImageUrl()
        );
    }
    private void publishOptionAudit(
            Long optionId,
            String optionName,
            List<CreateProductOptionValueRequest> optionValues,
            AuditAction action
    ) {
        if (optionId == null) return;

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
                case CREATE -> "THÊM MỚI TÙY CHỌN";
                case UPDATE -> "CẬP NHẬT TÙY CHỌN";
                case DELETE -> "XÓA TÙY CHỌN";
            };

            String detailValues = "";
            if (optionValues != null && !optionValues.isEmpty()) {
                String valueNames = optionValues.stream()
                        .map(CreateProductOptionValueRequest::valueName)
                        .collect(Collectors.joining(", "));
                detailValues = String.format(" [Bao gồm các giá trị: %s]", valueNames);
            }

            String message = String.format("[%s] %s đã thực hiện hành động %s: %s%s",
                    timeString, performerEmail, actionVn, optionName, detailValues);

            OptionAuditEvent event = new OptionAuditEvent(
                    optionId,
                    optionName,
                    action,
                    performerEmail,
                    currentTime,
                    message
            );

            kafkaObjectTemplate.send(auditLogTopic, event);
            log.info("Đã bắn audit event {} cho option id={}", action.name(), optionId);

        } catch (Exception ex) {
            log.warn("Failed to publish audit event cho optionId={}: {}", optionId, ex.getMessage());
        }
    }
}
