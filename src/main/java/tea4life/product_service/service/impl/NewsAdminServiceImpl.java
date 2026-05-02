package tea4life.product_service.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tea4life.product_service.advice.exception.ResourceNotFoundException;
import tea4life.product_service.client.StorageClient;
import tea4life.product_service.dto.base.ApiResponse;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.FileMoveRequest;
import tea4life.product_service.dto.request.NewsChunkRequest;
import tea4life.product_service.dto.request.NewsRequest;
import tea4life.product_service.dto.response.NewsDetailResponse;
import tea4life.product_service.dto.response.NewsSummaryResponse;
import tea4life.product_service.model.News;
import tea4life.product_service.model.NewsCategory;
import tea4life.product_service.model.NewsChunk;
import tea4life.product_service.model.enums.NewsContentType;
import tea4life.product_service.repository.news.NewsCategoryRepository;
import tea4life.product_service.repository.news.NewsRepository;
import tea4life.product_service.service.NewsAdminService;
import tea4life.product_service.utils.NewsMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Le Tran Gia Huy
 * @created 07/04/2026 - 2:27 PM
 * @project Tea4Life-Product-Service
 * @package tea4life.product_service.service.impl
 */

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsAdminServiceImpl implements NewsAdminService {
    NewsRepository newsRepository;
    NewsCategoryRepository categoryRepository;
    NewsMapper newsMapper;
    StorageClient storageClient;
    KafkaTemplate<String, String> kafkaTemplate;
    KafkaTemplate<String, Object> kafkaObjectTemplate;
    @Value("${spring.kafka.topic.storage-delete-file}")
    @NonFinal
    String storageDeleteFileTopic;

    //dùng để bắn sự kiện lên kafka và gửi đến audit server.
    @Value("${spring.kafka.topic.audit-log}")
    @NonFinal
    String auditLogTopic;

    public PageResponse<NewsSummaryResponse> findAll(Pageable pageable) {
        Page<@NonNull NewsSummaryResponse> responsePage = newsRepository.findAllNewsWithCategory(pageable)
                .map(newsMapper::mapToSummaryResponse);
        return new PageResponse<>(responsePage);
    }

    public PageResponse<NewsSummaryResponse> findByCategoryId(Long categoryId, Pageable pageable) {
        Page<@NonNull NewsSummaryResponse> responsePage = newsRepository
                .findAllByCategoryId(categoryId, pageable)
                .map(newsMapper::mapToSummaryResponse);
        return new PageResponse<>(responsePage);
    }

    public NewsDetailResponse findById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết!"));
        return newsMapper.mapToDetailResponse(news);
    }

    public NewsDetailResponse findBySlug(String slug) {
        News news = newsRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết!"));
        return newsMapper.mapToDetailResponse(news);
    }

    @Transactional
    public NewsDetailResponse create(NewsRequest request) {
        // Luồng Create: Khởi tạo Entity mới tinh, bỏ qua hoàn toàn khái niệm ID
        News news = new News();
        return processAndSaveNews(news, request);
    }

    @Transactional
    public NewsDetailResponse update(Long id, NewsRequest request) {
        // Luồng Update: Bắt buộc ID phải tồn tại, nếu sai ném lỗi 404 ngay lập tức
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết để cập nhật!"));

        return processAndSaveNews(news, request);
    }

    @Transactional
    public void delete(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết để xóa!"));
        newsRepository.delete(news);
    }

    // Hàm private chứa core logic
    private NewsDetailResponse processAndSaveNews(News news, NewsRequest request) {
        // 1. GIỮ LẠI URL CŨ TRƯỚC TIÊN (Bao gồm Thumbnail và các Chunk dạng ảnh)
        String oldThumbnailUrl = news.getThumbnailUrl();
        List<String> oldChunkImageUrls = news.getChunks().stream()
                .filter(chunk -> chunk.getType() == NewsContentType.IMAGE)
                .map(NewsChunk::getContent)
                .filter(this::hasText)
                .toList();
        // 2. Cập nhật thông tin cơ bản
        news.setTitle(request.title());
        news.setThumbnailUrl(request.thumbnailUrl());
        // 3. Cập nhật Category
        boolean isCategoryChanged = (news.getCategory() == null) ||
                (!news.getCategory().getId().equals(request.categoryId()));
        if (isCategoryChanged) {
            NewsCategory category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
            news.setCategory(category);
        }
        // 4. LƯU LẦN 1: BẮT BUỘC ĐỂ LẤY ID TỪ SNOWFLAKE (Cho cả Thumbnail và Chunk)
        news = newsRepository.save(news);
        // 5. XỬ LÝ FILE STORAGE CHO THUMBNAIL
        String finalThumbnailUrl = request.thumbnailUrl();
        if (hasText(request.thumbnailUrl()) && !request.thumbnailUrl().equals(oldThumbnailUrl)) {
            String destinationPath = "news/items/" + news.getId();
            ApiResponse<String> storageResponse = storageClient.confirmFile(
                    new FileMoveRequest(request.thumbnailUrl(), destinationPath)
            );
            if (storageResponse.getErrorCode() != null) {
                throw new RuntimeException("Lỗi di chuyển file thumbnail: " + storageResponse.getErrorMessage());
            }
            finalThumbnailUrl = storageResponse.getData();
            news.setThumbnailUrl(finalThumbnailUrl);
        }
        // 6. XỬ LÝ CHUNKS VÀ STORAGE CHO CHUNKS
        news.getChunks().clear();
        List<String> finalChunkImageUrls = new ArrayList<>(); // Danh sách chứa URL ảnh chốt hạ
        for (NewsChunkRequest chunkReq : request.chunks()) {
            NewsChunk newChunk = new NewsChunk();
            newChunk.setType(chunkReq.type());
            newChunk.setSortIndex(chunkReq.sortIndex());
            String finalContent = chunkReq.content();
            // Chỉ xử lý Storage nếu Chunk là dạng IMAGE và có nội dung
            if (chunkReq.type() == NewsContentType.IMAGE && hasText(finalContent)) {
                // Thuật toán tối ưu: Chỉ gọi API sang StorageClient nếu đây là ẢNH MỚI TINH.
                // Nếu URL gửi lên đã tồn tại trong list cũ -> User giữ nguyên ảnh -> Không làm gì cả.
                if (!oldChunkImageUrls.contains(finalContent)) {
                    String destinationPath = "news/items/" + news.getId() + "/chunks";
                    ApiResponse<String> storageResponse = storageClient.confirmFile(
                            new FileMoveRequest(finalContent, destinationPath)
                    );
                    if (storageResponse.getErrorCode() != null) {
                        throw new RuntimeException("Lỗi di chuyển file Chunk: " + storageResponse.getErrorMessage());
                    }
                    finalContent = storageResponse.getData(); // Lấy URL xịn (vĩnh viễn) từ Storage
                }
                // Lưu lại URL này vào danh sách chốt hạ để lát check với Kafka
                finalChunkImageUrls.add(finalContent);
            }
            newChunk.setContent(finalContent);
            news.addChunk(newChunk);
        }
        // 7. LƯU LẦN 2: CHỐT DATA CUỐI CÙNG XUỐNG DB
        news = newsRepository.save(news);
        // 8. BẮN KAFKA XÓA FILE CŨ TỒN ĐỌNG (Transaction an toàn)
        // 8.1 Dọn rác Thumbnail
        boolean isThumbnailChangedOrRemoved = oldThumbnailUrl != null && !oldThumbnailUrl.equals(finalThumbnailUrl);
        if (isThumbnailChangedOrRemoved) {
            publishStorageDelete(oldThumbnailUrl);
        }
        // 8.2 Dọn rác Chunk Images
        for (String oldImageUrl : oldChunkImageUrls) {
            // Nếu ảnh cũ KHÔNG CÒN xuất hiện trong danh sách ảnh cuối cùng -> User đã xóa chunk đó
            if (!finalChunkImageUrls.contains(oldImageUrl)) {
                publishStorageDelete(oldImageUrl);
            }
        }
        return newsMapper.mapToDetailResponse(news);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void publishStorageDelete(String fileUrl) {
        if (hasText(fileUrl)) {
            kafkaTemplate.send(storageDeleteFileTopic, fileUrl);
        }
    }
}
