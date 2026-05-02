package tea4life.product_service.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.client.RecommendationClient;
import tea4life.product_service.client.StorageClient;
import tea4life.product_service.context.UserContext;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.event.ProductAuditEvent;
import tea4life.product_service.dto.request.CreateProductRequest;
import tea4life.product_service.dto.request.FileMoveRequest;
import tea4life.product_service.dto.response.ProductPopularityResponse;
import tea4life.product_service.dto.response.ProductResponse;
import tea4life.product_service.model.Product;
import tea4life.product_service.model.ProductCategory;
import tea4life.product_service.model.ProductOption;
import tea4life.product_service.model.enums.AuditAction;
import tea4life.product_service.repository.product.ProductCategoryRepository;
import tea4life.product_service.repository.product.ProductOptionRepository;
import tea4life.product_service.repository.product.ProductRepository;
import tea4life.product_service.service.ProductAdminService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ProductAdminServiceImpl implements ProductAdminService {

    // Repository
    ProductRepository productRepository;
    ProductCategoryRepository productCategoryRepository;
    ProductOptionRepository productOptionRepository;

    // Client
    StorageClient storageClient;
    RecommendationClient recommendationClient;

    // Kafka
    KafkaTemplate<String, String> kafkaStringTemplate;
    KafkaTemplate<String, Object> kafkaObjectTemplate;

    // Topic
    @NonFinal
    String storageDeleteFileTopic;

    @NonFinal
    String auditLogTopic;

    public ProductAdminServiceImpl(
            ProductRepository productRepository,
            ProductCategoryRepository productCategoryRepository,
            ProductOptionRepository productOptionRepository,
            StorageClient storageClient,
            @Qualifier("kafkaStringTemplate") KafkaTemplate<String, String> kafkaStringTemplate,
            @Qualifier("kafkaObjectTemplate") KafkaTemplate<String, Object> kafkaObjectTemplate,
            RecommendationClient recommendationClient,
            @Value("${spring.kafka.topic.storage-delete-file}") String storageDeleteFileTopic,
            @Value("${spring.kafka.topic.audit-log}") String auditLogTopic
    ) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productOptionRepository = productOptionRepository;
        this.storageClient = storageClient;
        this.recommendationClient = recommendationClient;
        this.kafkaStringTemplate = kafkaStringTemplate;
        this.kafkaObjectTemplate = kafkaObjectTemplate;
        this.storageDeleteFileTopic = storageDeleteFileTopic;
        this.auditLogTopic = auditLogTopic;
    }

    /**
     * ========================================================
     * Create Product
     * ========================================================
     */
    @Override
    public ProductResponse createProduct(CreateProductRequest request) {

        // 1. Lấy thông tin từ request rồi lưu vào db
        Product product = new Product();
        mapToProduct(product, request);
        product = productRepository.save(product);

        // 2. Upload ảnh rồi cập nhật vào db
        if (hasText(request.imageKey())) {
            String destinationPath = "products/items/" + product.getId();

            ApiResponse<String> storageResponse = storageClient
                    .confirmFile(new FileMoveRequest(request.imageKey(), destinationPath));
            if (storageResponse.getErrorCode() != null)
                throw new RuntimeException("Lỗi di chuyển file: " + storageResponse.getErrorMessage());

            product.setImageUrl(storageResponse.getData());
            product = productRepository.save(product);
        }

        // 3. Bắn topic để audit
        publishProductAudit(product.getId(), product.getName(), AuditAction.CREATE);

        return mapToProductResponse(product, ProductPopularityResponse.empty(product.getId()));
    }

    /**
     * ========================================================
     * Get Products
     * ========================================================
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAllProducts(Pageable pageable) {

        Page<Product> productPage = productRepository.findAllBy(pageable);
        Map<Long, ProductPopularityResponse> popularityByProductId = getPopularityMap(productPage.getContent());

        Page<ProductResponse> responsePage = productPage.
                map(product -> mapToProductResponse(
                        product,
                        popularityByProductId.getOrDefault(product.getId(), ProductPopularityResponse.empty(product.getId()))
                ));
        return new PageResponse<>(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findProductById(Long id) {
        Product product = findProductEntityById(id);
        return mapToProductResponse(product, getProductPopularityOrDefault(product.getId()));
    }

    /**
     * ========================================================
     * Update Product
     * ========================================================
     */
    @Override
    public ProductResponse updateProduct(Long id, CreateProductRequest request) {
        Product product = findProductEntityById(id);
        String oldImageUrl = product.getImageUrl();

        mapToProduct(product, request);

        if (hasText(request.imageKey())) {
            String destinationPath = "products/items/" + product.getId();
            ApiResponse<String> storageResponse = storageClient.confirmFile(new FileMoveRequest(request.imageKey(), destinationPath));
            if (storageResponse.getErrorCode() != null) {
                throw new RuntimeException("Lỗi di chuyển file: " + storageResponse.getErrorMessage());
            }
            product.setImageUrl(storageResponse.getData());
        }

        Product saved = productRepository.save(product);
        if (hasText(request.imageKey()) && !Objects.equals(oldImageUrl, saved.getImageUrl())) {
            publishStorageDelete(oldImageUrl);
        }

        publishProductAudit(saved.getId(), saved.getName(), AuditAction.UPDATE);
        return mapToProductResponse(saved, getProductPopularityOrDefault(saved.getId()));
    }

    /**
     * ========================================================
     * Delete Product
     * ========================================================
     */
    @Override
    public void deleteProduct(Long id) {
        Product product = findProductEntityById(id);
        String imageUrl = product.getImageUrl();
        String productName = product.getName();
        productRepository.delete(product);
        publishStorageDelete(imageUrl);
        publishProductAudit(id, productName, AuditAction.DELETE);
    }

    /**
     * ========================================================
     * Mapping
     * ========================================================
     */
    private void mapToProduct(
            Product product,
            CreateProductRequest request
    ) {
        product.setProductCategory(
                findCategoryById(parseRequiredId(request.productCategoryId(), "productCategoryId"))
        );
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        product.setProductOptions(resolveProductOptions(request.productOptionIds()));
    }

    private ProductResponse mapToProductResponse(
            Product product,
            ProductPopularityResponse popularity
    ) {
        List<String> productOptionIds = product.getProductOptions() == null
                ? List.of()
                : product.getProductOptions().stream().map(option -> option.getId().toString()).toList();

        return new ProductResponse(
                product.getId().toString(),
                product.getProductCategory().getId().toString(),
                product.getProductCategory().getName(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getImageUrl(),
                productOptionIds,
                popularity
        );
    }

    /**
     * ========================================================
     * Lookup
     * ========================================================
     */
    private Product findProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm"));
    }

    private ProductCategory findCategoryById(Long categoryId) {
        return productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục sản phẩm"));
    }

    private List<ProductOption> resolveProductOptions(List<String> productOptionIds) {
        if (productOptionIds == null || productOptionIds.isEmpty())
            return List.of();

        List<Long> optionIds = productOptionIds.stream()
                .map(id -> parseRequiredId(id, "productOptionId"))
                .toList();

        List<ProductOption> options = productOptionRepository.findAllById(optionIds);
        Set<Long> foundIds = options
                .stream()
                .map(ProductOption::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (Long optionId : new HashSet<>(optionIds)) {
            if (!foundIds.contains(optionId)) {
                throw new EntityNotFoundException("Không tìm thấy Product Optoin này: " + optionId);
            }
        }

        return options;
    }

    private ProductPopularityResponse getProductPopularityOrDefault(Long productId) {
        try {
            ApiResponse<ProductPopularityResponse> response = recommendationClient
                    .getProductPopularity(productId);

            if (response != null && response.getData() != null)
                return response.getData();

        } catch (Exception ex) {
            log.warn("Khong the lay product popularity cho productId={}: {}", productId, ex.getMessage());
        }

        return ProductPopularityResponse.empty(productId);
    }

    private Map<Long, ProductPopularityResponse> getPopularityMap(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .toList();

        if (productIds.isEmpty()) {
            return Map.of();
        }

        try {
            ApiResponse<List<ProductPopularityResponse>> response = recommendationClient
                    .getProductPopularities(productIds);
            List<ProductPopularityResponse> popularities = response == null ? List.of() : response.getData();

            if (popularities == null || popularities.isEmpty()) {
                return buildDefaultPopularityMap(productIds);
            }

            Map<Long, ProductPopularityResponse> popularityMap = popularities.stream()
                    .filter(Objects::nonNull)
                    .filter(popularity -> hasText(popularity.productId()))
                    .collect(Collectors.toMap(
                            popularity -> Long.parseLong(popularity.productId()),
                            popularity -> popularity,
                            (left, right) -> right,
                            LinkedHashMap::new
                    ));

            for (Long productId : productIds) {
                popularityMap.putIfAbsent(productId, ProductPopularityResponse.empty(productId));
            }
            return popularityMap;
        } catch (Exception ex) {
            log.warn("Khong the lay product popularities: {}", ex.getMessage());
            return buildDefaultPopularityMap(productIds);
        }
    }

    private Map<Long, ProductPopularityResponse> buildDefaultPopularityMap(List<Long> productIds) {
        Map<Long, ProductPopularityResponse> popularityMap = new LinkedHashMap<>();
        for (Long productId : productIds) {
            popularityMap.put(productId, ProductPopularityResponse.empty(productId));
        }
        return popularityMap;
    }

    /**
     * ========================================================
     * Utils
     * ========================================================
     */
    private Long parseRequiredId(String rawId, String fieldName) {
        if (!hasText(rawId)) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }
        try {
            return Long.parseLong(rawId.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " không hợp lệ", ex);
        }
    }

    /**
     * ========================================================
     * Kafka Publish
     * ========================================================
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void publishStorageDelete(String fileUrl) {
        if (hasText(fileUrl)) {
            kafkaStringTemplate.send(storageDeleteFileTopic, fileUrl);
        }
    }

    private void publishProductAudit(Long productId, String productName, AuditAction action) {
        if (productId == null) return;

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

            // Chuyển action thành chữ tiếng Việt để ghép vào message
            String actionVn = switch (action) {
                case CREATE -> "THÊM MỚI SẢN PHẨM";
                case UPDATE -> "CẬP NHẬT SẢN PHẨM";
                case DELETE -> "XÓA SẢN PHẨM";
            };

            String message = String.format("[%s] %s đã thực hiện hành động %s: %s",
                    timeString, performerEmail, actionVn, productName);

            ProductAuditEvent event = new ProductAuditEvent(
                    productId,
                    productName,
                    action,
                    performerEmail,
                    currentTime,
                    message
            );

            kafkaObjectTemplate.send(auditLogTopic, event);
            log.info("Đã bắn audit event {} cho sản phẩm id={}", action.name(), productId);

        } catch (Exception ex) {
            log.warn("Failed to publish audit event cho productId={}: {}", productId, ex.getMessage());
        }
    }
}

