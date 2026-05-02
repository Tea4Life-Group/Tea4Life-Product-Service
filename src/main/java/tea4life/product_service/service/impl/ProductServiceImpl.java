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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tea4life.product_service.client.RecommendationClient;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.event.ProductClickedEvent;
import tea4life.product_service.dto.response.PopularProductCardResponse;
import tea4life.product_service.dto.response.ProductCategoryResponse;
import tea4life.product_service.dto.response.ProductDetailResponse;
import tea4life.product_service.dto.response.ProductOptionResponse;
import tea4life.product_service.dto.response.ProductOptionValueResponse;
import tea4life.product_service.dto.response.ProductPopularityResponse;
import tea4life.product_service.dto.response.ProductSummaryResponse;
import tea4life.product_service.model.Product;
import tea4life.product_service.model.ProductCategory;
import tea4life.product_service.model.ProductOption;
import tea4life.product_service.model.ProductOptionValue;
import tea4life.product_service.repository.product.ProductRepository;
import tea4life.product_service.service.ProductService;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ProductServiceImpl implements ProductService {

    static final int RANDOM_PRODUCT_LIMIT = 10;

    ProductRepository productRepository;
    KafkaTemplate<String, Object> kafkaTemplate;
    RecommendationClient recommendationClient;

    @Value("${spring.kafka.topic.product-clicked}")
    @NonFinal
    String productClickedTopic;

    public ProductServiceImpl(
            ProductRepository productRepository,
            @Qualifier("kafkaObjectTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            RecommendationClient recommendationClient
    ) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.recommendationClient = recommendationClient;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> findProducts(
            Pageable pageable,
            String keyword,
            Long categoryId,
            Double minPrice,
            Double maxPrice
    ) {
        validatePriceRange(minPrice, maxPrice);

        Page<@NonNull ProductSummaryResponse> responsePage = productRepository
                .findAllByFilters(normalizeKeyword(keyword), categoryId, minPrice, maxPrice, pageable)
                .map(this::toSummaryResponse);

        return new PageResponse<>(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse findProductById(Long id) {
        Product product = productRepository
                .findDetailById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm"));

        publishProductClicked(product.getId());
        return toDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PopularProductCardResponse> getPopularProducts(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        List<ProductPopularityResponse> popularities = getRecommendedPopularities(safeLimit);

        if (!popularities.isEmpty()) {
            List<Long> recommendedIds = popularities.stream()
                    .map(ProductPopularityResponse::productId)
                    .filter(StringUtils::hasText)
                    .map(Long::parseLong)
                    .toList();

            List<PopularProductCardResponse> enriched = enrichPopularProducts(recommendedIds, popularities);
            if (!enriched.isEmpty()) {
                return enriched;
            }
        }

        return fallbackPopularProducts(safeLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getRandomProducts() {
        return productRepository.findRandomActiveProducts(RANDOM_PRODUCT_LIMIT).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    private void publishProductClicked(Long productId) {
        if (productId == null) return;

        try {
            kafkaTemplate.send(productClickedTopic, new ProductClickedEvent(productId));
        } catch (Exception ex) {
            log.warn("Failed to publish product_clicked event for productId={}: {}", productId, ex.getMessage());
        }
    }

    private List<ProductPopularityResponse> getRecommendedPopularities(int limit) {
        try {
            ApiResponse<List<ProductPopularityResponse>> response = recommendationClient.getPopularProducts(limit);
            if (response == null || response.getData() == null) {
                return List.of();
            }
            return response.getData().stream().limit(limit).toList();
        } catch (Exception ex) {
            log.warn("Failed to get recommendation popular products: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<PopularProductCardResponse> enrichPopularProducts(
            List<Long> productIds,
            List<ProductPopularityResponse> popularities
    ) {
        if (productIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productById = productRepository.findByIdInAndActiveTrue(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<Long, ProductPopularityResponse> popularityByProductId = popularities.stream()
                .filter(popularity -> StringUtils.hasText(popularity.productId()))
                .collect(Collectors.toMap(
                        popularity -> Long.parseLong(popularity.productId()),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return productIds.stream()
                .map(productId -> {
                    Product product = productById.get(productId);
                    if (product == null) {
                        return null;
                    }
                    ProductPopularityResponse popularity = popularityByProductId.getOrDefault(
                            productId,
                            ProductPopularityResponse.empty(productId)
                    );
                    return toPopularCardResponse(product, popularity);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<PopularProductCardResponse> fallbackPopularProducts(int limit) {
        return productRepository.findByActiveTrueOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(product -> toPopularCardResponse(product, ProductPopularityResponse.empty(product.getId())))
                .toList();
    }

    private ProductSummaryResponse toSummaryResponse(Product product) {
        return new ProductSummaryResponse(
                product.getId().toString(),
                product.getName(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getProductCategory().getName()
        );
    }

    private PopularProductCardResponse toPopularCardResponse(Product product, ProductPopularityResponse popularity) {
        return new PopularProductCardResponse(
                product.getId().toString(),
                product.getName(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getProductCategory().getName(),
                popularity
        );
    }

    private ProductDetailResponse toDetailResponse(Product product) {
        List<ProductOptionResponse> productOptions = product.getProductOptions() == null
                ? List.of()
                : product.getProductOptions().stream()
                .sorted(Comparator.comparing(ProductOption::getSortOrder))
                .map(this::toOptionResponse)
                .toList();

        return new ProductDetailResponse(
                product.getId().toString(),
                toCategoryResponse(product.getProductCategory()),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getImageUrl(),
                productOptions
        );
    }

    private ProductCategoryResponse toCategoryResponse(ProductCategory category) {
        return ProductCategoryResponse.builder()
                .id(category.getId().toString())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .build();
    }

    private ProductOptionResponse toOptionResponse(ProductOption option) {
        List<ProductOptionValueResponse> productOptionValues = option.getProductOptionValues() == null
                ? List.of()
                : option.getProductOptionValues().stream()
                .sorted(Comparator.comparing(ProductOptionValue::getSortOrder))
                .map(this::toValueResponse)
                .toList();

        return new ProductOptionResponse(
                option.getId().toString(),
                option.getName(),
                option.isRequired(),
                option.isMultiSelect(),
                option.getSortOrder(),
                productOptionValues
        );
    }

    private ProductOptionValueResponse toValueResponse(ProductOptionValue value) {
        return new ProductOptionValueResponse(
                value.getId().toString(),
                value.getProductOption().getId().toString(),
                value.getValueName(),
                value.getExtraPrice(),
                value.getSortOrder(),
                value.getImageUrl()
        );
    }

    private void validatePriceRange(Double minPrice, Double maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice)
            throw new IllegalArgumentException("minPrice must be less than or equal to maxPrice");
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;

        String trimmedKeyword = keyword.trim();
        return trimmedKeyword.isEmpty() ? null : trimmedKeyword;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 10;
        }
        return Math.max(1, Math.min(limit, 20));
    }
}
