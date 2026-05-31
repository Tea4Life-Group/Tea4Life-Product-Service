package tea4life.product_service.favorite.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.context.UserContext;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.response.ProductSummaryResponse;
import tea4life.product_service.product.model.Product;
import tea4life.product_service.favorite.model.ProductFavorite;
import tea4life.product_service.favorite.repository.ProductFavoriteRepository;
import tea4life.product_service.product.repository.ProductRepository;
import tea4life.product_service.favorite.service.ProductFavoriteService;

/**
 * @author : user664dntp
 * @mailto : phatdang19052004@gmail.com
 * @created : 8/05/2026, Friday
 **/
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ProductFavoriteServiceImpl implements ProductFavoriteService {
    ProductFavoriteRepository productFavoriteRepository;
    ProductRepository productRepository;

    @Override
    public void addFavorite(Long productId) {
        String keycloakId = getUserKeycloakId();

        if (productFavoriteRepository.existsByUserKeycloakIdAndProductId(keycloakId, productId)) {
            throw new DataIntegrityViolationException("Sản phẩm đã nằm trong danh sách yêu thích");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm"));

        ProductFavorite favorite = ProductFavorite.builder()
                .userKeycloakId(keycloakId)
                .product(product)
                .build();

        productFavoriteRepository.save(favorite);

        // TODO: Bắn event Kafka giống như product-clicked (Ví dụ: product-favorited)
    }

    @Override
    public void removeFavorite(Long productId) {
        String keycloakId = getUserKeycloakId();

        productFavoriteRepository.findByUserKeycloakIdAndProductId(keycloakId, productId)
                .ifPresent(productFavoriteRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getMyFavorites(Pageable pageable) {
        String keycloakId = getUserKeycloakId();

        Page<ProductSummaryResponse> page = productFavoriteRepository
                .findByUserKeycloakIdOrderByCreatedAtDesc(keycloakId, pageable)
                .map(favorite -> toSummaryResponse(favorite.getProduct()));

        return new PageResponse<>(page);
    }

    private String getUserKeycloakId() {
        String keycloakId = UserContext.get().getKeycloakId();
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new RuntimeException("Unauthorized: Không tìm thấy thông tin định danh");
        }
        return keycloakId;
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
}



