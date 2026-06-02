package tea4life.product_service.ai_chat.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tea4life.product_service.client.BlogReviewClient;
import tea4life.product_service.context.UserContext;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.ProductAiChatRequest;
import tea4life.product_service.dto.response.ProductAiCartOptionSelectionResponse;
import tea4life.product_service.dto.response.ProductAiCartSuggestionResponse;
import tea4life.product_service.dto.response.ProductAiChatConfigResponse;
import tea4life.product_service.dto.response.ProductAiChatHistoryItemResponse;
import tea4life.product_service.dto.response.ProductAiChatResponse;
import tea4life.product_service.dto.response.ProductSummaryResponse;
import tea4life.product_service.option.model.ProductOption;
import tea4life.product_service.option.model.ProductOptionValue;
import tea4life.product_service.product.model.Product;
import tea4life.product_service.ai_chat.model.ProductAiChatMessage;
import tea4life.product_service.ai_chat.model.ProductAiChatSettings;
import tea4life.product_service.ai_chat.repository.ProductAiChatMessageRepository;
import tea4life.product_service.ai_chat.repository.ProductAiChatSettingsRepository;
import tea4life.product_service.product.repository.ProductRepository;
import tea4life.product_service.ai_chat.service.ProductAiChatService;

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductAiChatServiceImpl implements ProductAiChatService {

    static final long SETTINGS_ID = 1L;
    static final String LIMIT_REACHED_ANSWER = "Bạn đã dùng hết lượt hỏi AI trong hôm nay. Vui lòng quay lại vào ngày mai.";
    static final String FALLBACK_ANSWER = "Mình chưa thể kết nối AI lúc này. Bạn thử lại sau ít phút nhé.";
    static final Set<String> STOP_WORDS = Set.of(
            "toi", "minh", "ban", "la", "co", "khong", "nhe", "voi", "cho", "xin",
            "muon", "can", "giup", "tu", "van", "san", "pham", "tra", "sua", "trai",
            "va", "hoac", "the", "nao", "nhu", "nay", "kia", "duoc", "day", "an", "uong"
    );
    static final Set<String> PRICE_WORD_HINTS = Set.of("gia", "vnd", "dong", "k", "nghin", "ngan");
    static final Set<String> HIGHEST_PRICE_HINTS = Set.of("cao nhat", "dat nhat", "max");
    static final Set<String> LOWEST_PRICE_HINTS = Set.of("thap nhat", "re nhat", "min");
    static final List<String> LOW_SWEET_QUERY_HINTS = List.of(
            "it ngot", "khong ngot", "khong thich ngot", "giam duong", "it duong", "less sugar"
    );
    static final List<String> MENU_LIST_QUERY_HINTS = List.of(
            "danh sach san pham",
            "tat ca san pham",
            "toan bo san pham",
            "menu",
            "thuc don",
            "xem san pham",
            "cua hang co gi"
    );
    static final List<String> CART_ACTION_QUERY_HINTS = List.of(
            "them vao gio",
            "bo vao gio",
            "dua vao gio",
            "cho vao gio",
            "them gio hang",
            "dat mon",
            "order",
            "mua mon",
            "lay mon",
            "chon mon giup",
            "chon giup",
            "chot don"
    );
    static final List<String> NATURAL_ORDER_QUERY_HINTS = List.of(
            "cho toi mot",
            "cho minh mot",
            "lay cho toi",
            "lay cho minh",
            "lam cho toi",
            "lam cho minh",
            "toi lay",
            "minh lay"
    );
    static final List<String> HIGH_RATED_QUERY_HINTS = List.of(
            "danh gia cao", "nhieu sao", "sao cao", "rating cao", "review tot", "tot nhat"
    );
    static final List<String> LOW_RATED_QUERY_HINTS = List.of(
            "danh gia thap", "it sao", "sao thap", "rating thap", "review kem", "te nhat"
    );
    static final List<String> HAS_RATED_PRODUCTS_QUERY_HINTS = List.of(
            "co danh gia",
            "duoc danh gia",
            "co review",
            "da review",
            "tu khach hang",
            "tu nguoi dung"
    );
    static final Set<String> SAVORY_HINTS = Set.of(
            "banh", "banh trang", "an vat", "snack", "sa te", "muoi", "cha bong", "kho ga", "kho bo"
    );
    static final Set<String> SWEET_DRINK_HINTS = Set.of(
            "tra sua", "milk tea", "sua", "kem", "socola", "chocolate", "caramel", "mat ong", "matcha"
    );
    static final Set<String> LOW_SUGAR_PRODUCT_HINTS = Set.of(
            "it duong", "khong duong", "less sugar", "thanh nhiet"
    );
    static final Set<String> NON_PRODUCT_HINTS = Set.of(
            "ngot", "dang", "chat", "beo", "it", "nhieu", "bao", "gia",
            "co", "khong", "la", "nao", "duoc", "khuyen", "mai", "size", "nong", "da",
            "danh", "sach", "ben", "cua", "hang", "list", "menu", "thuc", "don"
    );
    static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    ProductRepository productRepository;
    BlogReviewClient blogReviewClient;
    ProductAiChatMessageRepository messageRepository;
    ProductAiChatSettingsRepository settingsRepository;
    ObjectMapper objectMapper;
    RestClient restClient;

    @Value("${ai.gemini.api-key:}")
    @NonFinal
    String geminiApiKey;

    @Value("${ai.gemini.model:gemini-2.0-flash}")
    @NonFinal
    String geminiModel;

    @Value("${ai.chat.catalog-size:20}")
    @NonFinal
    int catalogSize;

    @Value("${ai.chat.history-size:100}")
    @NonFinal
    int historySize;

    @Value("${ai.chat.prompt-history-size:5}")
    @NonFinal
    int promptHistorySize;

    @Value("${ai.chat.max-output-tokens:600}")
    @NonFinal
    int maxOutputTokens;

    @Value("${ai.chat.temperature:0.4}")
    @NonFinal
    double temperature;

    @Value("${ai.chat.default-display-name:Tea4Life AI}")
    @NonFinal
    String defaultDisplayName;

    @Value("${ai.chat.default-max-questions-per-user-per-day:20}")
    @NonFinal
    int defaultMaxQuestionsPerUserPerDay;

    @Value("${ai.gemini.retry.max-attempts:3}")
    @NonFinal
    int geminiRetryMaxAttempts;

    @Value("${ai.gemini.retry.initial-backoff-ms:1000}")
    @NonFinal
    long geminiRetryInitialBackoffMs;

    @Value("${ai.gemini.retry.max-backoff-ms:8000}")
    @NonFinal
    long geminiRetryMaxBackoffMs;

    public ProductAiChatServiceImpl(
            ProductRepository productRepository,
            BlogReviewClient blogReviewClient,
            ProductAiChatMessageRepository messageRepository,
            ProductAiChatSettingsRepository settingsRepository,
            ObjectMapper objectMapper,
            @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl
    ) {
        this.productRepository = productRepository;
        this.blogReviewClient = blogReviewClient;
        this.messageRepository = messageRepository;
        this.settingsRepository = settingsRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @PostConstruct
    void logAiConfigAtStartup() {
        boolean hasApiKey = StringUtils.hasText(geminiApiKey);
        log.info(
                "AI config loaded - hasApiKey={}, apiKey={}, model={}, catalogSize={}, maxOutputTokens={}, temperature={}",
                hasApiKey,
                geminiApiKey,
                geminiModel,
                catalogSize,
                maxOutputTokens,
                temperature
        );
    }

    @Override
    @Transactional
    public ProductAiChatResponse chat(ProductAiChatRequest request) {
        ProductAiChatSettings settings = getOrCreateSettings();
        UserIdentity user = getCurrentUser();
        int maxPerDay = Math.max(0, settings.getMaxQuestionsPerUserPerDay() == null ? defaultMaxQuestionsPerUserPerDay : settings.getMaxQuestionsPerUserPerDay());

        Long askedToday = getAskedCountToday(user.keycloakId());
        Integer remainingBeforeAsk = maxPerDay <= 0 ? null : Math.max(0, maxPerDay - askedToday.intValue());

        String message = request.message().trim();
        List<Product> candidateProducts = findCandidateProducts(message);

        if (maxPerDay > 0 && remainingBeforeAsk != null && remainingBeforeAsk <= 0) {
            List<ProductSummaryResponse> recommendedProducts = List.of();
            saveMessage(user, message, normalizeQuestion(message), LIMIT_REACHED_ANSWER, true);
            return new ProductAiChatResponse(
                    LIMIT_REACHED_ANSWER,
                    recommendedProducts,
                    List.of(),
                    false,
                    settings.getChatboxDisplayName(),
                    maxPerDay,
                    0,
                    true
            );
        }

        String answer;
        if (!StringUtils.hasText(geminiApiKey)) {
            answer = FALLBACK_ANSWER;
        } else {
            String productContext = buildProductContext(candidateProducts);
            String recentConversationContext = buildRecentConversationContext(
                    loadRecentMessagesForPrompt(user.keycloakId())
            );
            answer = askGemini(message, productContext, recentConversationContext);
        }

        List<ProductSummaryResponse> recommendedProducts = buildRecommendedProducts(message, answer, candidateProducts);
        boolean cartActionRequested = asksCartAction(message, candidateProducts);
        List<ProductAiCartSuggestionResponse> cartSuggestions = buildCartSuggestions(
                message,
                candidateProducts,
                recommendedProducts,
                cartActionRequested
        );
        answer = alignAnswerWithRecommendedProducts(message, answer, recommendedProducts);
        answer = alignAnswerWithCartAction(message, answer, cartSuggestions, cartActionRequested);
        answer = sanitizeAssistantAnswer(answer);
        saveMessage(user, message, normalizeQuestion(message), answer, false);
        Integer remainingAfterAsk = maxPerDay <= 0 ? null : Math.max(0, maxPerDay - (askedToday.intValue() + 1));

        return new ProductAiChatResponse(
                answer,
                recommendedProducts,
                cartSuggestions,
                cartActionRequested,
                settings.getChatboxDisplayName(),
                maxPerDay,
                remainingAfterAsk,
                false
        );
    }

    @Override
    @Transactional
    public ProductAiChatConfigResponse getPublicConfig() {
        ProductAiChatSettings settings = getOrCreateSettings();
        return new ProductAiChatConfigResponse(
                settings.getChatboxDisplayName(),
                settings.getMaxQuestionsPerUserPerDay()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductAiChatHistoryItemResponse> getHistory(int page, int size) {
        UserIdentity user = getCurrentUser();
        if (!StringUtils.hasText(user.keycloakId())) {
            return emptyHistoryPage(page, size);
        }

        int safePageNumber = Math.max(1, page);
        int safePageSize = safeHistoryPageSize(size);
        int pageIndex = safePageNumber - 1;

        Page<ProductAiChatMessage> historyPage = messageRepository.findRecentByUserKeycloakId(
                user.keycloakId(),
                PageRequest.of(pageIndex, safePageSize)
        );
        if (historyPage.isEmpty()) {
            return emptyHistoryPage(safePageNumber, safePageSize);
        }

        List<ProductAiChatHistoryItemResponse> historyItems = new ArrayList<>(historyPage.getContent().stream()
                .map(message -> {
                    List<ProductSummaryResponse> recommendedProducts;
                    if (message.isLimitReached()) {
                        recommendedProducts = List.of();
                    } else {
                        List<Product> candidates = findCandidateProducts(message.getQuestion());
                        recommendedProducts = buildRecommendedProducts(
                                message.getQuestion(),
                                message.getAnswer(),
                                candidates
                        );
                        String alignedAnswer = alignAnswerWithRecommendedProducts(
                                message.getQuestion(),
                                message.getAnswer(),
                                recommendedProducts
                        );
                        return new ProductAiChatHistoryItemResponse(
                                message.getQuestion(),
                                sanitizeAssistantAnswer(alignedAnswer),
                                recommendedProducts,
                                message.getCreatedAt()
                        );
                    }
                    return new ProductAiChatHistoryItemResponse(
                            message.getQuestion(),
                            sanitizeAssistantAnswer(message.getAnswer()),
                            recommendedProducts,
                            message.getCreatedAt()
                    );
                })
                .toList());
        Collections.reverse(historyItems);

        return PageResponse.<ProductAiChatHistoryItemResponse>builder()
                .content(historyItems)
                .page(historyPage.getNumber() + 1)
                .size(historyPage.getSize())
                .totalElements(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .hasMore(historyPage.hasNext())
                .build();
    }

    private List<Product> findCandidateProducts(String message) {
        int safeCatalogSize = safeCatalogSize();
        boolean highestPriceIntent = asksHighestPrice(message);
        boolean lowestPriceIntent = asksLowestPrice(message);
        boolean highestRatedIntent = asksHighestRated(message);
        boolean lowestRatedIntent = asksLowestRated(message);
        boolean ratedProductsIntent = asksRatedProducts(message);
        boolean lowSweetIntent = asksLowSweetPreference(message);
        boolean browseCatalogIntent = asksBrowseCatalog(message);
        String normalizedMessage = normalizeForMatch(message);

        List<Product> catalog = loadCatalogForAi(safeCatalogSize);
        if (catalog.isEmpty()) {
            return List.of();
        }

        if (browseCatalogIntent) {
            return catalog.stream().limit(safeCatalogSize).toList();
        }
        if (highestPriceIntent) {
            return catalog.stream()
                    .sorted(Comparator.comparing(
                                    (Product product) -> product.getBasePrice() == null ? 0D : product.getBasePrice())
                            .reversed())
                    .limit(safeCatalogSize)
                    .toList();
        }
        if (lowestPriceIntent) {
            return catalog.stream()
                    .sorted(Comparator.comparing(
                            product -> product.getBasePrice() == null ? Double.MAX_VALUE : product.getBasePrice()))
                    .limit(safeCatalogSize)
                    .toList();
        }
        if (highestRatedIntent) {
            return pickExtremeRatedProducts(catalog, true);
        }
        if (lowestRatedIntent) {
            return pickExtremeRatedProducts(catalog, false);
        }
        if (ratedProductsIntent) {
            return pickProductsWithAnyRatings(catalog, safeCatalogSize);
        }

        Set<String> queryTokens = extractQueryTokens(normalizedMessage);
        List<Product> rankedProducts = catalog.stream()
                .map(product -> new ScoredProduct(product, scoreProductForMessage(product, normalizedMessage, queryTokens)))
                .filter(scoredProduct -> scoredProduct.score() > 0)
                .sorted(Comparator.comparingInt(ScoredProduct::score).reversed())
                .limit(safeCatalogSize)
                .map(ScoredProduct::product)
                .toList();

        if (!rankedProducts.isEmpty()) {
            return rankedProducts;
        }

        if (lowSweetIntent) {
            return findLowSweetCandidates(catalog, safeCatalogSize);
        }

        if (queryTokens.isEmpty()) {
            return catalog.stream().limit(safeCatalogSize).toList();
        }

        return catalog.stream().limit(safeCatalogSize).toList();
    }

    private List<Product> loadCatalogForAi(int safeCatalogSize) {
        List<Product> products = productRepository.findByActiveTrue();
        if (!products.isEmpty()) {
            return products;
        }

        int sampleSize = Math.max(120, safeCatalogSize * 8);
        return productRepository.findByActiveTrueOrderByCreatedAtDesc(PageRequest.of(0, sampleSize)).getContent();
    }

    private List<Product> findLowSweetCandidates(List<Product> catalog, int safeCatalogSize) {
        return catalog.stream()
                .sorted(Comparator
                        .comparingInt(this::lowSweetScore)
                        .thenComparing(product -> product.getBasePrice() == null ? 0D : product.getBasePrice()))
                .limit(safeCatalogSize)
                .toList();
    }

    private Set<String> extractQueryTokens(String normalizedMessage) {
        if (!StringUtils.hasText(normalizedMessage)) {
            return Set.of();
        }

        LinkedHashSet<String> queryTokens = new LinkedHashSet<>();
        for (String token : normalizedMessage.split("\\s+")) {
            if (token.length() < 2
                    || STOP_WORDS.contains(token)
                    || NON_PRODUCT_HINTS.contains(token)) {
                continue;
            }
            queryTokens.add(token);
            if (queryTokens.size() >= 12) {
                break;
            }
        }
        return queryTokens;
    }

    private int scoreProductForMessage(Product product, String normalizedMessage, Set<String> queryTokens) {
        String categoryName = product.getProductCategory() == null ? "" : product.getProductCategory().getName();
        String normalizedProductName = normalizeForMatch(product.getName());
        String searchable = normalizeForMatch(
                safe(product.getName()) + " " + safe(categoryName) + " " + safe(product.getDescription())
        );

        if (!StringUtils.hasText(searchable)) {
            return 0;
        }

        int score = 0;
        if (StringUtils.hasText(normalizedProductName) && normalizedMessage.contains(normalizedProductName)) {
            score += 100;
        }

        for (String token : queryTokens) {
            if (searchable.contains(token)) {
                boolean nameToken = normalizedProductName.contains(token);
                score += (token.length() >= 5 ? 5 : 3) + (nameToken ? 4 : 0);
            }
        }

        if (productNameAppearsInMessage(product, normalizedMessage)) {
            score += 30;
        }

        return score;
    }

    private List<ProductSummaryResponse> buildRecommendedProducts(
            String userMessage,
            String answer,
            List<Product> candidateProducts
    ) {
        if (candidateProducts == null || candidateProducts.isEmpty()) {
            return List.of();
        }

        int limit = Math.min(6, safeCatalogSize());
        if (asksHighestPrice(userMessage)) {
            return pickExtremePriceProducts(candidateProducts, true).stream()
                    .limit(limit)
                    .map(this::toSummaryResponse)
                    .toList();
        }
        if (asksLowestPrice(userMessage)) {
            return pickExtremePriceProducts(candidateProducts, false).stream()
                    .limit(limit)
                    .map(this::toSummaryResponse)
                    .toList();
        }
        if (asksHighestRated(userMessage)) {
            return pickExtremeRatedProducts(candidateProducts, true).stream()
                    .limit(Math.max(1, limit))
                    .map(this::toSummaryResponse)
                    .toList();
        }
        if (asksLowestRated(userMessage)) {
            return pickExtremeRatedProducts(candidateProducts, false).stream()
                    .limit(Math.max(1, limit))
                    .map(this::toSummaryResponse)
                    .toList();
        }
        if (asksRatedProducts(userMessage)) {
            return pickProductsWithAnyRatings(candidateProducts, limit).stream()
                    .map(this::toSummaryResponse)
                    .toList();
        }
        if (asksLowSweetPreference(userMessage)
                && candidateProducts.stream().noneMatch(product -> productNameAppearsInMessage(product, normalizeForMatch(userMessage)))) {
            return candidateProducts.stream()
                    .sorted(Comparator.comparingInt(this::lowSweetScore))
                    .limit(limit)
                    .map(this::toSummaryResponse)
                    .toList();
        }

        List<Product> matchedProducts = findMentionedProducts(answer, candidateProducts);
        if (matchedProducts.isEmpty()) {
            return candidateProducts.stream()
                    .limit(Math.min(3, limit))
                    .map(this::toSummaryResponse)
                    .toList();
        }
        return matchedProducts.stream()
                .limit(limit)
                .map(this::toSummaryResponse)
                .toList();
    }

    private List<ProductAiCartSuggestionResponse> buildCartSuggestions(
            String userMessage,
            List<Product> candidateProducts,
            List<ProductSummaryResponse> recommendedProducts,
            boolean cartActionRequested
    ) {
        if (candidateProducts == null || candidateProducts.isEmpty()) {
            return List.of();
        }

        List<ProductOrderMention> orderMentions = findOrderMentions(userMessage, candidateProducts);
        int itemLimit = cartActionRequested
                ? Math.min(4, Math.max(1, orderMentions.isEmpty()
                ? requestedDistinctItemCount(userMessage)
                : orderMentions.size()))
                : Math.min(3, Math.max(1, recommendedProducts == null ? 0 : recommendedProducts.size()));

        if (cartActionRequested && !orderMentions.isEmpty()) {
            return orderMentions.stream()
                    .limit(itemLimit)
                    .map(mention -> toCartSuggestion(
                            mention.product(),
                            mention.messageScope(),
                            requestedQuantityForProduct(userMessage, mention, 1)
                    ))
                    .toList();
        }

        int quantity = cartActionRequested ? requestedQuantityPerItem(userMessage) : 1;
        List<Product> products = alignProductsForCartSuggestions(candidateProducts, recommendedProducts, itemLimit);

        return products.stream()
                .limit(itemLimit)
                .map(product -> toCartSuggestion(product, userMessage, quantity))
                .toList();
    }

    private List<ProductOrderMention> findOrderMentions(
            String userMessage,
            List<Product> candidateProducts
    ) {
        String normalizedMessage = normalizeForMatch(userMessage);
        if (!StringUtils.hasText(normalizedMessage)) {
            return List.of();
        }

        List<ProductOrderMention> mentions = candidateProducts.stream()
                .map(product -> {
                    int startIndex = productNameStartIndex(product, normalizedMessage);
                    return startIndex < 0 ? null : new ProductOrderMention(product, startIndex, "");
                })
                .filter(mention -> mention != null)
                .sorted(Comparator.comparingInt(ProductOrderMention::startIndex))
                .collect(Collectors.toCollection(ArrayList::new));

        if (mentions.isEmpty()) {
            return List.of();
        }

        List<ProductOrderMention> scopedMentions = new ArrayList<>();
        for (int i = 0; i < mentions.size(); i++) {
            ProductOrderMention mention = mentions.get(i);
            int nextStartIndex = i + 1 < mentions.size()
                    ? mentions.get(i + 1).startIndex()
                    : normalizedMessage.length();
            int scopeStart = findOrderScopeStart(normalizedMessage, mention.startIndex());
            String scope = normalizedMessage.substring(scopeStart, nextStartIndex).trim();
            scopedMentions.add(new ProductOrderMention(mention.product(), mention.startIndex(), scope));
        }

        return scopedMentions;
    }

    private int productNameStartIndex(Product product, String normalizedMessage) {
        String normalizedName = normalizeForMatch(product.getName());
        if (!StringUtils.hasText(normalizedName) || !StringUtils.hasText(normalizedMessage)) {
            return -1;
        }

        int exactIndex = normalizedMessage.indexOf(normalizedName);
        if (exactIndex >= 0) {
            return exactIndex;
        }

        int bestIndex = -1;
        int matchedTokens = 0;
        boolean matchedDistinctiveToken = false;
        for (String token : normalizedName.split("\\s+")) {
            if (token.length() < 3 || STOP_WORDS.contains(token) || NON_PRODUCT_HINTS.contains(token)) {
                continue;
            }
            int tokenIndex = normalizedMessage.indexOf(token);
            if (tokenIndex >= 0) {
                matchedTokens++;
                if (isDistinctiveProductToken(token)) {
                    matchedDistinctiveToken = true;
                }
                bestIndex = bestIndex < 0 ? tokenIndex : Math.min(bestIndex, tokenIndex);
            }
        }
        return matchedTokens >= 2 && matchedDistinctiveToken ? bestIndex : -1;
    }

    private boolean isDistinctiveProductToken(String token) {
        return token.length() >= 4
                && !Set.of("hong", "tra", "sua", "luc", "xanh", "den", "mon").contains(token);
    }

    private int findOrderScopeStart(String normalizedMessage, int productStartIndex) {
        int scopeStart = 0;
        String[] separators = {" va them ", " va ", " them ", " cho toi ", " cho minh "};
        for (String separator : separators) {
            int separatorIndex = normalizedMessage.lastIndexOf(separator, productStartIndex);
            if (separatorIndex >= 0) {
                scopeStart = Math.max(scopeStart, separatorIndex + 1);
            }
        }
        return scopeStart;
    }

    private List<Product> alignProductsForCartSuggestions(
            List<Product> candidateProducts,
            List<ProductSummaryResponse> recommendedProducts,
            int limit
    ) {
        Map<String, Product> candidateById = candidateProducts.stream()
                .collect(Collectors.toMap(
                        product -> String.valueOf(product.getId()),
                        product -> product,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        List<Product> aligned = recommendedProducts == null
                ? new ArrayList<>()
                : recommendedProducts.stream()
                .map(product -> candidateById.get(product.id()))
                .filter(product -> product != null)
                .limit(limit)
                .collect(Collectors.toCollection(ArrayList::new));

        if (aligned.size() >= limit) {
            return aligned;
        }

        for (Product product : candidateProducts) {
            if (aligned.stream().noneMatch(item -> item.getId().equals(product.getId()))) {
                aligned.add(product);
            }
            if (aligned.size() >= limit) {
                break;
            }
        }

        return aligned;
    }

    private ProductAiCartSuggestionResponse toCartSuggestion(
            Product product,
            String userMessage,
            int quantity
    ) {
        return new ProductAiCartSuggestionResponse(
                String.valueOf(product.getId()),
                product.getName(),
                product.getImageUrl(),
                product.getBasePrice() == null ? 0D : product.getBasePrice(),
                Math.max(1, quantity),
                chooseOptionSelections(product, userMessage)
        );
    }

    private List<ProductAiCartOptionSelectionResponse> chooseOptionSelections(
            Product product,
            String userMessage
    ) {
        if (product.getProductOptions() == null || product.getProductOptions().isEmpty()) {
            return List.of();
        }

        String normalizedMessage = normalizeForMatch(userMessage);
        List<ProductAiCartOptionSelectionResponse> selections = new ArrayList<>();

        product.getProductOptions().stream()
                .sorted(Comparator.comparing(ProductOption::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .forEach(option -> {
                    List<ProductOptionValue> values = option.getProductOptionValues() == null
                            ? List.of()
                            : option.getProductOptionValues().stream()
                            .sorted(Comparator.comparing(ProductOptionValue::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                            .toList();
                    if (values.isEmpty()) {
                        return;
                    }

                    ProductOptionValue semanticValue = inferSemanticOptionValue(option, values, normalizedMessage);
                    List<ProductOptionValue> matchedValues = values.stream()
                            .filter(value -> {
                                String normalizedValueName = normalizeForMatch(value.getValueName());
                                return StringUtils.hasText(normalizedValueName)
                                        && normalizedValueName.length() >= 3
                                        && normalizedMessage.contains(normalizedValueName);
                            })
                            .toList();

                    if (option.isMultiSelect()) {
                        List<ProductOptionValue> selectedValues = matchedValues.isEmpty()
                                ? (semanticValue != null
                                ? List.of(semanticValue)
                                : (option.isRequired() ? List.of(values.get(0)) : List.of()))
                                : matchedValues;
                        selectedValues.forEach(value -> selections.add(toCartOptionSelection(option, value)));
                        return;
                    }

                    ProductOptionValue selectedValue = semanticValue != null
                            ? semanticValue
                            : (matchedValues.isEmpty() ? null : matchedValues.get(0));
                    if (selectedValue == null && option.isRequired()) {
                        selectedValue = values.get(0);
                    }
                    if (selectedValue != null) {
                        selections.add(toCartOptionSelection(option, selectedValue));
                    }
                });

        return selections;
    }

    private ProductOptionValue inferSemanticOptionValue(
            ProductOption option,
            List<ProductOptionValue> values,
            String normalizedMessage
    ) {
        if (!StringUtils.hasText(normalizedMessage) || values == null || values.isEmpty()) {
            return null;
        }

        String normalizedOptionName = normalizeForMatch(option.getName());
        String normalizedValues = values.stream()
                .map(value -> normalizeForMatch(value.getValueName()))
                .collect(Collectors.joining(" "));

        if (asksLowSweetPreference(normalizedMessage)
                && (looksLikeSugarOption(normalizedOptionName) || looksLikeSugarOption(normalizedValues))) {
            return pickPreferredValue(values, List.of(
                    "it duong",
                    "it ngot",
                    "less sugar",
                    "30",
                    "25",
                    "50",
                    "khong duong",
                    "khong ngot",
                    "0"
            ));
        }

        if (asksMediumSweetPreference(normalizedMessage)
                && (looksLikeSugarOption(normalizedOptionName) || looksLikeSugarOption(normalizedValues))) {
            return pickPreferredValue(values, List.of(
                    "50",
                    "vua",
                    "duong vua",
                    "ngot vua",
                    "medium"
            ));
        }

        if (asksLowIcePreference(normalizedMessage)
                && (looksLikeIceOption(normalizedOptionName) || looksLikeIceOption(normalizedValues))) {
            if (normalizedMessage.contains("khong da")) {
                ProductOptionValue noIceValue = pickPreferredValue(values, List.of("khong da", "no ice", "0"));
                if (noIceValue != null) {
                    return noIceValue;
                }
            }
            return pickPreferredValue(values, List.of(
                    "it da",
                    "less ice",
                    "30",
                    "50",
                    "khong da",
                    "0"
            ));
        }

        if (asksHighIcePreference(normalizedMessage)
                && (looksLikeIceOption(normalizedOptionName) || looksLikeIceOption(normalizedValues))) {
            return pickPreferredValue(values, List.of(
                    "nhieu da",
                    "full da",
                    "100",
                    "70",
                    "75",
                    "more ice"
            ));
        }

        if (asksMediumIcePreference(normalizedMessage)
                && (looksLikeIceOption(normalizedOptionName) || looksLikeIceOption(normalizedValues))) {
            return pickPreferredValue(values, List.of(
                    "50",
                    "binh thuong",
                    "thuong",
                    "vua",
                    "da vua",
                    "medium",
                    "normal"
            ));
        }

        if (looksLikeSizeOption(normalizedOptionName) || looksLikeSizeOption(normalizedValues)) {
            ProductOptionValue sizeValue = inferSizeOptionValue(values, normalizedMessage);
            if (sizeValue != null) {
                return sizeValue;
            }
        }

        return null;
    }

    private ProductOptionValue inferSizeOptionValue(
            List<ProductOptionValue> values,
            String normalizedMessage
    ) {
        if (normalizedMessage.contains("co l")
                || normalizedMessage.contains("size l")
                || normalizedMessage.contains("large")
                || normalizedMessage.contains("lon")) {
            return pickPreferredValue(values, List.of("l", "large", "lon"));
        }
        if (normalizedMessage.contains("co m")
                || normalizedMessage.contains("size m")
                || normalizedMessage.contains("medium")
                || normalizedMessage.contains("vua")) {
            return pickPreferredValue(values, List.of("m", "medium", "vua"));
        }
        if (normalizedMessage.contains("co s")
                || normalizedMessage.contains("size s")
                || normalizedMessage.contains("small")
                || normalizedMessage.contains("nho")) {
            return pickPreferredValue(values, List.of("s", "small", "nho"));
        }
        return null;
    }

    private ProductOptionValue pickPreferredValue(
            List<ProductOptionValue> values,
            List<String> preferredHints
    ) {
        for (String hint : preferredHints) {
            String normalizedHint = normalizeForMatch(hint);
            for (ProductOptionValue value : values) {
                String normalizedValueName = normalizeForMatch(value.getValueName());
                if (StringUtils.hasText(normalizedValueName) && normalizedValueName.contains(normalizedHint)) {
                    return value;
                }
            }
        }
        return null;
    }

    private ProductAiCartOptionSelectionResponse toCartOptionSelection(
            ProductOption option,
            ProductOptionValue value
    ) {
        return new ProductAiCartOptionSelectionResponse(
                String.valueOf(option.getId()),
                option.getName(),
                String.valueOf(value.getId()),
                value.getValueName(),
                value.getExtraPrice() == null ? 0D : value.getExtraPrice()
        );
    }

    private String alignAnswerWithRecommendedProducts(
            String userMessage,
            String currentAnswer,
            List<ProductSummaryResponse> recommendedProducts
    ) {
        int recommendedCount = recommendedProducts == null ? 0 : recommendedProducts.size();

        if (asksHighestRated(userMessage)) {
            if (recommendedCount <= 0) {
                return "Hiện tại chưa có dữ liệu đánh giá để xác định sản phẩm có đánh giá cao nhất.";
            }
            return String.format(
                    Locale.ROOT,
                    "Dựa trên đánh giá của khách hàng, hiện tại có %d sản phẩm có đánh giá cao nhất.",
                    recommendedCount
            );
        }

        if (asksLowestRated(userMessage)) {
            if (recommendedCount <= 0) {
                return "Hiện tại chưa có dữ liệu đánh giá để xác định sản phẩm có đánh giá thấp nhất.";
            }
            return String.format(
                    Locale.ROOT,
                    "Dựa trên đánh giá của khách hàng, hiện tại có %d sản phẩm có đánh giá thấp nhất.",
                    recommendedCount
            );
        }
        if (asksRatedProducts(userMessage)) {
            if (recommendedCount <= 0) {
                return "Hiện tại chưa có sản phẩm nào có đánh giá từ khách hàng.";
            }
            return String.format(
                    Locale.ROOT,
                    "Hiện tại có %d sản phẩm đã có đánh giá từ khách hàng.",
                    recommendedCount
            );
        }

        return currentAnswer;
    }

    private String alignAnswerWithCartAction(
            String userMessage,
            String currentAnswer,
            List<ProductAiCartSuggestionResponse> cartSuggestions,
            boolean cartActionRequested
    ) {
        if (!cartActionRequested) {
            return currentAnswer;
        }

        if (cartSuggestions == null || cartSuggestions.isEmpty()) {
            return "Mình chưa tìm được món phù hợp để thêm vào giỏ. Bạn nói rõ tên món hoặc khẩu vị hơn một chút nhé.";
        }

        String itemNames = cartSuggestions.stream()
                .map(item -> item.quantity() + " x " + item.productName())
                .collect(Collectors.joining(", "));
        String suffix = "Mình đã chọn: " + itemNames + ".";
        if (!StringUtils.hasText(currentAnswer) || FALLBACK_ANSWER.equals(currentAnswer)) {
            return suffix;
        }
        if (normalizeForMatch(currentAnswer).contains(normalizeForMatch(itemNames))) {
            return currentAnswer;
        }
        return currentAnswer + "\n\n" + suffix;
    }

    private List<Product> pickExtremePriceProducts(List<Product> products, boolean highest) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

        List<Product> sorted = new ArrayList<>(products.stream()
                .sorted(Comparator.comparing(
                        product -> product.getBasePrice() == null
                                ? (highest ? 0D : Double.MAX_VALUE)
                                : product.getBasePrice()))
                .toList());

        if (highest) {
            Collections.reverse(sorted);
        }

        Double targetPrice = null;
        for (Product product : sorted) {
            if (product.getBasePrice() != null) {
                targetPrice = product.getBasePrice();
                break;
            }
        }

        if (targetPrice == null) {
            return sorted.stream().limit(1).toList();
        }

        final double selectedTargetPrice = targetPrice;
        final double epsilon = 0.0001D;
        return sorted.stream()
                .filter(product -> product.getBasePrice() != null
                        && Math.abs(product.getBasePrice() - selectedTargetPrice) < epsilon)
                .toList();
    }

    private List<Product> findMentionedProducts(String answer, List<Product> products) {
        if (!StringUtils.hasText(answer) || products == null || products.isEmpty()) {
            return List.of();
        }

        String normalizedAnswer = normalizeForMatch(answer);
        if (!StringUtils.hasText(normalizedAnswer)) {
            return List.of();
        }

        return products.stream()
                .filter(product -> {
                    String normalizedName = normalizeForMatch(product.getName());
                    if (normalizedName.length() < 3) {
                        return false;
                    }
                    if (normalizedAnswer.contains(normalizedName)) {
                        return true;
                    }
                    int tokenMatched = 0;
                    for (String token : normalizedName.split("\\s+")) {
                        if (token.length() < 4 || STOP_WORDS.contains(token) || NON_PRODUCT_HINTS.contains(token)) {
                            continue;
                        }
                        if (normalizedAnswer.contains(token)) {
                            tokenMatched++;
                            if (tokenMatched >= 2) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .toList();
    }

    private List<Product> sortProductsByRating(List<Product> products, boolean highest, int limit) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductRatingSnapshot> ratingByProductId = loadProductRatingStats(products);
        Comparator<Product> comparator = Comparator
                .comparingDouble((Product product) ->
                        ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).averageRating())
                .thenComparingLong(product ->
                        ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).reviewCount());

        List<Product> sorted = products.stream()
                .sorted(highest ? comparator.reversed() : comparator)
                .limit(Math.max(1, limit))
                .toList();

        return sorted;
    }

    private List<Product> pickExtremeRatedProducts(List<Product> products, boolean highest) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductRatingSnapshot> ratingByProductId = loadProductRatingStats(products);
        final double epsilon = 0.0001D;

        List<Product> ratedProducts = products.stream()
                .filter(product ->
                        ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).reviewCount() > 0)
                .toList();

        if (ratedProducts.isEmpty()) {
            return sortProductsByRating(products, highest, Math.min(products.size(), 3));
        }

        double extremeRating = highest
                ? ratedProducts.stream()
                .mapToDouble(product -> ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).averageRating())
                .max()
                .orElse(0D)
                : ratedProducts.stream()
                .mapToDouble(product -> ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).averageRating())
                .min()
                .orElse(0D);

        Comparator<Product> tieBreaker = Comparator.comparingLong(
                (Product product) -> ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).reviewCount()
        ).reversed().thenComparing(product -> safe(product.getName()));

        return ratedProducts.stream()
                .filter(product -> Math.abs(
                        ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).averageRating() - extremeRating
                ) < epsilon)
                .sorted(tieBreaker)
                .toList();
    }

    private List<Product> pickProductsWithAnyRatings(List<Product> products, int limit) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductRatingSnapshot> ratingByProductId = loadProductRatingStats(products);
        Comparator<Product> byRatingAndReviews = Comparator
                .comparingDouble((Product product) ->
                        ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).averageRating())
                .reversed()
                .thenComparing(
                        Comparator.comparingLong((Product product) ->
                                ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).reviewCount())
                                .reversed()
                )
                .thenComparing(product -> safe(product.getName()));

        return products.stream()
                .filter(product -> ratingByProductId
                        .getOrDefault(product.getId(), ProductRatingSnapshot.empty())
                        .reviewCount() > 0)
                .sorted(byRatingAndReviews)
                .limit(Math.max(1, limit))
                .toList();
    }

    private Map<Long, ProductRatingSnapshot> loadProductRatingStats(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }

        try {
            var response = blogReviewClient.getProductRatingStats(productIds);
            if (response == null || response.getData() == null) {
                return Map.of();
            }

            Map<Long, ProductRatingSnapshot> result = new java.util.HashMap<>();
            response.getData().forEach(item -> {
                Long productId = parseProductId(item.productId());
                if (productId <= 0) {
                    return;
                }
                result.putIfAbsent(
                        productId,
                        new ProductRatingSnapshot(
                                sanitizeAverageRating(item.averageRating()),
                                Math.max(0L, item.reviewCount())
                        )
                );
            });
            return result;
        } catch (Exception ex) {
            log.warn("Khong the tai thong ke rating tu blog-service: {}", ex.getMessage());
            return Map.of();
        }
    }

    private Long parseProductId(String value) {
        if (!StringUtils.hasText(value)) {
            return -1L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private double sanitizeAverageRating(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0D;
        }
        return Math.max(0D, Math.min(5D, value));
    }

    private String buildProductContext(List<Product> products) {
        if (products.isEmpty()) {
            return "Khong tim thay san pham phu hop voi cau hoi.";
        }

        Map<Long, ProductRatingSnapshot> ratingByProductId = loadProductRatingStats(products);

        return products.stream()
                .map(product -> String.format(
                        Locale.ROOT,
                        "- id: %s | ten: %s | danh_muc: %s | gia: %.0f VND | danh_gia_tb: %.2f/5 | so_luot_danh_gia: %d | mo_ta: %s | goi_y_do_ngot: %s | tuy_chon: %s",
                        product.getId(),
                        safe(product.getName()),
                        product.getProductCategory() == null ? "Khong ro" : safe(product.getProductCategory().getName()),
                        product.getBasePrice() == null ? 0 : product.getBasePrice(),
                        ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).averageRating(),
                        ratingByProductId.getOrDefault(product.getId(), ProductRatingSnapshot.empty()).reviewCount(),
                        safe(product.getDescription()),
                        inferSweetnessHint(product),
                        buildOptionContext(product)
                ))
                .collect(Collectors.joining("\n"));
    }

    private String buildOptionContext(Product product) {
        if (product.getProductOptions() == null || product.getProductOptions().isEmpty()) {
            return "Khong co tuy chon";
        }

        return product.getProductOptions().stream()
                .sorted(Comparator.comparing(ProductOption::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(option -> {
                    String values = option.getProductOptionValues() == null || option.getProductOptionValues().isEmpty()
                            ? "Khong co gia tri"
                            : option.getProductOptionValues().stream()
                            .sorted(Comparator.comparing(ProductOptionValue::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                            .map(value -> String.format(
                                    Locale.ROOT,
                                    "%s(+%.0f)",
                                    safe(value.getValueName()),
                                    value.getExtraPrice() == null ? 0D : value.getExtraPrice()
                            ))
                            .collect(Collectors.joining(", "));
                    return String.format(
                            Locale.ROOT,
                            "%s[%s%s]: %s",
                            safe(option.getName()),
                            option.isRequired() ? "bat_buoc" : "khong_bat_buoc",
                            option.isMultiSelect() ? ", chon_nhieu" : "",
                            values
                    );
                })
                .collect(Collectors.joining("; "));
    }

    private String askGemini(
            String userMessage,
            String productContext,
            String recentConversationContext
    ) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text",
                                """
                                Ban la tro ly tu van san pham cho Tea4Life.
                                Quy tac bat buoc:
                                1) CHI duoc tu van dua tren danh sach SAN_PHAM_LIEN_QUAN duoc cung cap.
                                2) Neu khong tim thay san pham phu hop, noi ro va goi y nguoi dung mo rong tieu chi.
                                3) Khong bịa them ten san pham, gia, thanh phan.
                                4) Tra loi bang tieng Viet, gon gang, de doc.
                                5) Neu nguoi dung hoi ve muc do ngot ma mo ta san pham thieu, duoc phep suy luan tu TEN, DANH_MUC va truong goi_y_do_ngot.
                                6) Khi goi y, hay NEU TEN SAN PHAM cu the dung y trong SAN_PHAM_LIEN_QUAN.
                                7) Co the tham khao LICH_SU_HOI_THOAI_GAN_NHAT de giu ngu canh, nhung KHONG duoc mau thuan voi cau hoi hien tai.
                                8) Neu cau hoi lien quan den danh gia cao/thap, uu tien dua tren danh_gia_tb va so_luot_danh_gia.
                                9) Neu nguoi dung muon dat mon, chon mon, them vao gio hang, hay noi ro mon va tuy_chon/topping phu hop tu SAN_PHAM_LIEN_QUAN.
                                """
                        ))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of(
                                "text",
                                "CAU_HOI:\n" + userMessage
                                        + "\n\nLICH_SU_HOI_THOAI_GAN_NHAT:\n" + recentConversationContext
                                        + "\n\nSAN_PHAM_LIEN_QUAN:\n" + productContext
                        ))
                )),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", safeMaxOutputTokens()
                )
        );

        int maxAttempts = Math.max(1, geminiRetryMaxAttempts);
        long backoffMillis = Math.max(200L, geminiRetryInitialBackoffMs);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String rawResponse = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1beta/models/{model}:generateContent")
                                .queryParam("key", geminiApiKey)
                                .build(geminiModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                if (!StringUtils.hasText(rawResponse)) {
                    return FALLBACK_ANSWER;
                }

                JsonNode response = objectMapper.readTree(rawResponse);
                String text = extractText(response);
                if (StringUtils.hasText(text)) {
                    return text.trim();
                }
                return FALLBACK_ANSWER;
            } catch (RestClientResponseException ex) {
                int statusCode = ex.getStatusCode().value();
                boolean canRetry = isRetryableStatus(statusCode) && attempt < maxAttempts;
                if (canRetry) {
                    log.warn(
                            "Gemini API loi tam thoi status={}, thu lai lan {}/{} sau {}ms",
                            statusCode,
                            attempt + 1,
                            maxAttempts,
                            backoffMillis
                    );
                    sleepBeforeRetry(backoffMillis);
                    backoffMillis = nextBackoffMillis(backoffMillis);
                    continue;
                }
                log.warn("Khong the goi Gemini API (status={}): {}", statusCode, ex.getMessage());
                return FALLBACK_ANSWER;
            } catch (Exception ex) {
                boolean canRetry = attempt < maxAttempts;
                if (canRetry) {
                    log.warn(
                            "Loi ket noi Gemini (lan {}/{}): {}; thu lai sau {}ms",
                            attempt,
                            maxAttempts,
                            ex.getMessage(),
                            backoffMillis
                    );
                    sleepBeforeRetry(backoffMillis);
                    backoffMillis = nextBackoffMillis(backoffMillis);
                    continue;
                }
                log.warn("Khong the goi Gemini API: {}", ex.getMessage());
                return FALLBACK_ANSWER;
            }
        }

        return FALLBACK_ANSWER;
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 503
                || statusCode == 504;
    }

    private long nextBackoffMillis(long currentBackoffMillis) {
        long safeMaxBackoff = Math.max(200L, geminiRetryMaxBackoffMs);
        return Math.min(safeMaxBackoff, currentBackoffMillis * 2);
    }

    private void sleepBeforeRetry(long backoffMillis) {
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractText(JsonNode response) {
        if (response == null || !response.has("candidates")) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        JsonNode candidates = response.get("candidates");
        for (JsonNode candidate : candidates) {
            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray()) {
                continue;
            }
            for (JsonNode part : parts) {
                String text = part.path("text").asText(null);
                if (StringUtils.hasText(text)) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(text.trim());
                }
            }
            if (!builder.isEmpty()) {
                break;
            }
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    private String sanitizeAssistantAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }

        String sanitized = answer
                .replace("\r\n", "\n")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("`([^`]+)`", "$1")
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s*[*+-]\\s+", "- ")
                .replaceAll("(?m)^\\s*\\d+\\.\\s+", "- ")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        return sanitized;
    }

    private ProductSummaryResponse toSummaryResponse(Product product) {
        return new ProductSummaryResponse(
                String.valueOf(product.getId()),
                product.getName(),
                product.getBasePrice(),
                product.getImageUrl(),
                product.getProductCategory() == null ? null : product.getProductCategory().getName()
        );
    }

    private void saveMessage(
            UserIdentity user,
            String question,
            String normalizedQuestion,
            String answer,
            boolean limitReached
    ) {
        try {
            ProductAiChatMessage message = new ProductAiChatMessage();
            message.setUserKeycloakId(user.keycloakId());
            message.setUserEmail(user.email());
            message.setQuestion(question);
            message.setNormalizedQuestion(normalizedQuestion);
            message.setAnswer(answer);
            message.setLimitReached(limitReached);
            messageRepository.save(message);
        } catch (Exception ex) {
            log.warn("Khong the luu lich su ai chat: {}", ex.getMessage());
        }
    }

    private ProductAiChatSettings getOrCreateSettings() {
        return settingsRepository.findById(SETTINGS_ID)
                .orElseGet(() -> {
                    ProductAiChatSettings settings = new ProductAiChatSettings();
                    settings.setId(SETTINGS_ID);
                    settings.setChatboxDisplayName(normalizeDisplayName(defaultDisplayName));
                    settings.setMaxQuestionsPerUserPerDay(Math.max(0, defaultMaxQuestionsPerUserPerDay));
                    return settingsRepository.save(settings);
                });
    }

    private UserIdentity getCurrentUser() {
        UserContext context = UserContext.get();
        if (context == null) {
            return new UserIdentity(null, null);
        }
        return new UserIdentity(
                normalizeNullable(context.getKeycloakId()),
                normalizeNullable(context.getEmail())
        );
    }

    private Long getAskedCountToday(String userKeycloakId) {
        if (!StringUtils.hasText(userKeycloakId)) {
            return 0L;
        }
        Instant startOfDay = ZonedDateTime.now(VN_ZONE).toLocalDate().atStartOfDay(VN_ZONE).toInstant();
        return messageRepository.countByUserKeycloakIdAndCreatedAtGreaterThanEqualAndActiveTrue(userKeycloakId, startOfDay);
    }

    private String normalizeQuestion(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = normalizeForMatch(value);
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    private String normalizeForMatch(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean asksHighestPrice(String message) {
        String normalized = normalizeForMatch(message);
        return containsAny(normalized, HIGHEST_PRICE_HINTS)
                && containsAny(normalized, PRICE_WORD_HINTS);
    }

    private boolean asksLowestPrice(String message) {
        String normalized = normalizeForMatch(message);
        return containsAny(normalized, LOWEST_PRICE_HINTS)
                && containsAny(normalized, PRICE_WORD_HINTS);
    }

    private boolean asksLowSweetPreference(String message) {
        String normalized = normalizeForMatch(message);
        return containsAnyPhrase(normalized, LOW_SWEET_QUERY_HINTS)
                || (normalized.contains("khong") && normalized.contains("ngot"))
                || (normalized.contains("it") && normalized.contains("ngot"));
    }

    private boolean asksMediumSweetPreference(String message) {
        String normalized = normalizeForMatch(message);
        return normalized.contains("duong vua")
                || normalized.contains("ngot vua")
                || normalized.contains("do ngot vua")
                || normalized.contains("sugar 50")
                || (normalized.contains("duong") && normalized.contains("50"))
                || (normalized.contains("ngot") && normalized.contains("50"));
    }

    private boolean asksLowIcePreference(String message) {
        String normalized = normalizeForMatch(message);
        return normalized.contains("it da")
                || normalized.contains("khong da")
                || normalized.contains("giam da")
                || normalized.contains("less ice")
                || (normalized.contains("it") && normalized.contains("da"))
                || (normalized.contains("khong") && normalized.contains("da"));
    }

    private boolean asksMediumIcePreference(String message) {
        String normalized = normalizeForMatch(message);
        return normalized.contains("da vua")
                || normalized.contains("da cung vua")
                || normalized.contains("ice 50")
                || (normalized.contains("da") && normalized.contains("50"));
    }

    private boolean asksHighIcePreference(String message) {
        String normalized = normalizeForMatch(message);
        return normalized.contains("nhieu da")
                || normalized.contains("day da")
                || normalized.contains("full da")
                || normalized.contains("more ice")
                || (normalized.contains("da") && normalized.contains("100"));
    }

    private boolean asksHighestRated(String message) {
        String normalized = normalizeForMatch(message);
        boolean hasRatingWord = normalized.contains("danh gia")
                || normalized.contains("sao")
                || normalized.contains("rating")
                || normalized.contains("review");
        return containsAnyPhrase(normalized, HIGH_RATED_QUERY_HINTS)
                || (normalized.contains("danh gia") && normalized.contains("cao"))
                || (normalized.contains("nhieu") && normalized.contains("sao"))
                || (normalized.contains("cao nhat") && hasRatingWord);
    }

    private boolean asksLowestRated(String message) {
        String normalized = normalizeForMatch(message);
        boolean hasRatingWord = normalized.contains("danh gia")
                || normalized.contains("sao")
                || normalized.contains("rating")
                || normalized.contains("review");
        return containsAnyPhrase(normalized, LOW_RATED_QUERY_HINTS)
                || (normalized.contains("danh gia") && normalized.contains("thap"))
                || (normalized.contains("it") && normalized.contains("sao"))
                || (normalized.contains("thap nhat") && hasRatingWord);
    }

    private boolean asksRatedProducts(String message) {
        String normalized = normalizeForMatch(message);
        boolean hasRatingWord = normalized.contains("danh gia")
                || normalized.contains("sao")
                || normalized.contains("rating")
                || normalized.contains("review");
        boolean hasProductWord = normalized.contains("san pham")
                || normalized.contains("mon")
                || normalized.contains("thuc uong")
                || normalized.contains("do an");
        return containsAnyPhrase(normalized, HAS_RATED_PRODUCTS_QUERY_HINTS)
                || (hasRatingWord && hasProductWord);
    }

    private boolean asksBrowseCatalog(String message) {
        String normalized = normalizeForMatch(message);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        if (containsAnyPhrase(normalized, MENU_LIST_QUERY_HINTS)) {
            return true;
        }
        return normalized.contains("san pham")
                && (normalized.contains("tat ca")
                || normalized.contains("danh sach")
                || normalized.contains("menu")
                || normalized.contains("thuc don")
                || normalized.contains("cua hang"));
    }

    private boolean asksCartAction(String message, List<Product> candidateProducts) {
        String normalized = normalizeForMatch(message);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        boolean explicitCartAction = containsAnyPhrase(normalized, CART_ACTION_QUERY_HINTS)
                || (normalized.contains("gio hang") && normalized.contains("them"))
                || (normalized.contains("gio") && normalized.contains("them"))
                || (normalized.contains("mua") && (normalized.contains("mon") || normalized.contains("ly")))
                || (normalized.contains("dat") && (normalized.contains("mon") || normalized.contains("ly")))
                || (normalized.contains("chon") && normalized.contains("giup"));
        if (explicitCartAction) {
            return true;
        }

        boolean naturalOrder = containsAnyPhrase(normalized, NATURAL_ORDER_QUERY_HINTS)
                || Pattern.compile("\\bcho\\s+(toi|minh)\\s+\\d+\\b").matcher(normalized).find()
                || Pattern.compile("\\bcho\\s+(toi|minh)\\s+(mot|hai|ba|bon|nam)\\b").matcher(normalized).find();
        if (!naturalOrder || asksBrowseCatalog(message)) {
            return false;
        }

        return candidateProducts != null
                && candidateProducts.stream().anyMatch(product -> productNameAppearsInMessage(product, normalized));
    }

    private boolean productNameAppearsInMessage(Product product, String normalizedMessage) {
        String normalizedName = normalizeForMatch(product.getName());
        if (!StringUtils.hasText(normalizedName) || !StringUtils.hasText(normalizedMessage)) {
            return false;
        }
        if (normalizedMessage.contains(normalizedName)) {
            return true;
        }

        int matchedTokens = 0;
        for (String token : normalizedName.split("\\s+")) {
            if (token.length() < 3 || STOP_WORDS.contains(token) || NON_PRODUCT_HINTS.contains(token)) {
                continue;
            }
            if (normalizedMessage.contains(token)) {
                matchedTokens++;
                if (matchedTokens >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean looksLikeSugarOption(String value) {
        return value.contains("duong")
                || value.contains("ngot")
                || value.contains("sugar");
    }

    private boolean looksLikeIceOption(String value) {
        return value.contains(" da")
                || value.equals("da")
                || value.contains("ice");
    }

    private boolean looksLikeSizeOption(String value) {
        return value.contains("size")
                || value.contains("kich co")
                || value.contains("co ly")
                || value.equals("co")
                || value.contains(" size")
                || value.contains(" l ")
                || value.contains(" m ")
                || value.contains(" s ");
    }

    private int requestedDistinctItemCount(String message) {
        String normalized = normalizeForMatch(message);
        int parsedNumber = firstRequestedNumber(normalized);
        if (parsedNumber <= 1) {
            return 1;
        }
        if (normalized.contains("mon khac")
                || normalized.contains("mon khac nhau")
                || normalized.contains("loai")
                || normalized.contains("san pham")) {
            return Math.min(4, parsedNumber);
        }
        return 1;
    }

    private int requestedQuantityPerItem(String message) {
        String normalized = normalizeForMatch(message);
        int parsedNumber = firstRequestedNumber(normalized);
        if (parsedNumber <= 1) {
            return 1;
        }
        if (normalized.contains("ly")
                || normalized.contains("coc")
                || normalized.contains("phan")
                || normalized.contains("suat")) {
            return Math.min(10, parsedNumber);
        }
        return requestedDistinctItemCount(message) > 1 ? 1 : Math.min(10, parsedNumber);
    }

    private int requestedQuantityForProduct(
            String userMessage,
            ProductOrderMention mention,
            int defaultQuantity
    ) {
        String scope = StringUtils.hasText(mention.messageScope())
                ? mention.messageScope()
                : userMessage;
        int parsedNumber = firstRequestedNumber(scope);
        if (parsedNumber > 1) {
            return Math.min(10, parsedNumber);
        }
        if (scope.contains("mot")) {
            return 1;
        }
        return Math.max(1, defaultQuantity);
    }

    private int firstRequestedNumber(String normalizedMessage) {
        if (!StringUtils.hasText(normalizedMessage)) {
            return 1;
        }

        Matcher matcher = Pattern.compile("\\b(\\d{1,2})\\b").matcher(normalizedMessage);
        if (matcher.find()) {
            try {
                return Math.max(1, Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }

        Map<String, Integer> wordNumbers = Map.of(
                "mot", 1,
                "hai", 2,
                "ba", 3,
                "bon", 4,
                "nam", 5
        );
        for (Map.Entry<String, Integer> entry : wordNumbers.entrySet()) {
            if (normalizedMessage.contains(" " + entry.getKey() + " ")
                    || normalizedMessage.startsWith(entry.getKey() + " ")
                    || normalizedMessage.endsWith(" " + entry.getKey())) {
                return entry.getValue();
            }
        }
        return 1;
    }

    private boolean containsAny(String value, Set<String> hints) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String hint : hints) {
            if (value.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyPhrase(String value, List<String> hints) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String hint : hints) {
            if (StringUtils.hasText(hint) && value.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private int lowSweetScore(Product product) {
        String categoryName = product.getProductCategory() == null ? "" : product.getProductCategory().getName();
        String combined = normalizeForMatch(
                safe(product.getName()) + " " + safe(categoryName) + " " + safe(product.getDescription())
        );
        int score = 0;
        if (containsAny(combined, SAVORY_HINTS)) {
            score -= 4;
        }
        if (combined.contains("banh trang")) {
            score -= 5;
        }
        if (containsAny(combined, LOW_SUGAR_PRODUCT_HINTS)) {
            score -= 3;
        }
        if (containsAny(combined, SWEET_DRINK_HINTS)) {
            score += 4;
        }
        return score;
    }

    private String inferSweetnessHint(Product product) {
        int score = lowSweetScore(product);
        if (score <= -4) {
            return "uu_tien_it_ngot";
        }
        if (score <= 0) {
            return "co_the_it_ngot";
        }
        return "co_xu_huong_ngot";
    }

    private List<ProductAiChatMessage> loadRecentMessagesForPrompt(String userKeycloakId) {
        if (!StringUtils.hasText(userKeycloakId)) {
            return List.of();
        }
        Page<ProductAiChatMessage> page = messageRepository.findRecentByUserKeycloakId(
                userKeycloakId,
                PageRequest.of(0, safePromptHistorySize())
        );
        return page.getContent();
    }

    private String buildRecentConversationContext(List<ProductAiChatMessage> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return "Khong co lich su hoi thoai gan day.";
        }

        List<ProductAiChatMessage> orderedMessages = new ArrayList<>(recentMessages);
        Collections.reverse(orderedMessages);

        return orderedMessages.stream()
                .map(message -> String.format(
                        Locale.ROOT,
                        "Q: %s%nA: %s",
                        safe(message.getQuestion()),
                        safe(message.getAnswer())
                ))
                .collect(Collectors.joining("\n\n"));
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeDisplayName(String value) {
        if (!StringUtils.hasText(value)) {
            return "Tea4Life AI";
        }
        return value.trim();
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.replace("\n", " ").trim() : "Khong co";
    }

    private int safeCatalogSize() {
        return Math.max(1, catalogSize);
    }

    private int safeHistoryPageSize(int requestedSize) {
        int maxAllowed = Math.max(5, safeHistorySize());
        return Math.max(5, Math.min(requestedSize, maxAllowed));
    }

    private int safePromptHistorySize() {
        return Math.max(1, Math.min(5, promptHistorySize));
    }

    private int safeHistorySize() {
        return Math.max(10, historySize);
    }

    private PageResponse<ProductAiChatHistoryItemResponse> emptyHistoryPage(int page, int size) {
        int safePageNumber = Math.max(1, page);
        int safePageSize = Math.max(1, size);
        return PageResponse.<ProductAiChatHistoryItemResponse>builder()
                .content(List.of())
                .page(safePageNumber)
                .size(safePageSize)
                .totalElements(0)
                .totalPages(0)
                .hasMore(false)
                .build();
    }

    private int safeMaxOutputTokens() {
        return Math.max(64, maxOutputTokens);
    }

    private record ScoredProduct(Product product, int score) {
    }

    private record ProductOrderMention(Product product, int startIndex, String messageScope) {
    }

    private record ProductRatingSnapshot(double averageRating, long reviewCount) {
        static ProductRatingSnapshot empty() {
            return new ProductRatingSnapshot(0D, 0L);
        }
    }

    private record UserIdentity(String keycloakId, String email) {
    }
}



