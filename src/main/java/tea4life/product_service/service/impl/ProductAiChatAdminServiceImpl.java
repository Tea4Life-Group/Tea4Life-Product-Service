package tea4life.product_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.util.StringUtils;
import tea4life.product_service.dto.base.PageResponse;
import tea4life.product_service.dto.request.UpdateProductAiChatConfigRequest;
import tea4life.product_service.dto.response.ProductAiChatConfigResponse;
import tea4life.product_service.dto.response.ProductAiMonthlyQuestionResponse;
import tea4life.product_service.dto.response.ProductAiChatOverviewResponse;
import tea4life.product_service.dto.response.ProductAiTopQuestionResponse;
import tea4life.product_service.dto.response.ProductAiUserUsageResponse;
import tea4life.product_service.model.ProductAiChatSettings;
import tea4life.product_service.repository.product.ProductAiChatMessageRepository;
import tea4life.product_service.repository.product.ProductAiChatSettingsRepository;
import tea4life.product_service.service.ProductAiChatAdminService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductAiChatAdminServiceImpl implements ProductAiChatAdminService {

    static final long SETTINGS_ID = 1L;
    static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    ProductAiChatMessageRepository messageRepository;
    ProductAiChatSettingsRepository settingsRepository;
    ObjectMapper objectMapper;
    RestClient restClient;

    @Value("${ai.chat.default-display-name:Tea4Life AI}")
    @NonFinal
    String defaultDisplayName;

    @Value("${ai.chat.default-max-questions-per-user-per-day:20}")
    @NonFinal
    int defaultMaxQuestionsPerUserPerDay;

    @Value("${ai.gemini.api-key:}")
    @NonFinal
    String geminiApiKey;

    @Value("${ai.gemini.model:gemini-2.0-flash}")
    @NonFinal
    String geminiModel;

    @Value("${ai.chat.admin.topic-analysis-sample-size:120}")
    @NonFinal
    int topicAnalysisSampleSize;

    @Value("${ai.chat.admin.topic-analysis-max-input-characters:8000}")
    @NonFinal
    int topicAnalysisMaxInputCharacters;

    @Value("${ai.gemini.retry.max-attempts:3}")
    @NonFinal
    int geminiRetryMaxAttempts;

    @Value("${ai.gemini.retry.initial-backoff-ms:1000}")
    @NonFinal
    long geminiRetryInitialBackoffMs;

    @Value("${ai.gemini.retry.max-backoff-ms:8000}")
    @NonFinal
    long geminiRetryMaxBackoffMs;

    public ProductAiChatAdminServiceImpl(
            ProductAiChatMessageRepository messageRepository,
            ProductAiChatSettingsRepository settingsRepository,
            ObjectMapper objectMapper,
            @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl
    ) {
        this.messageRepository = messageRepository;
        this.settingsRepository = settingsRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public ProductAiChatConfigResponse getConfig() {
        ProductAiChatSettings settings = getOrCreateSettings();
        return toConfigResponse(settings);
    }

    @Override
    @Transactional
    public ProductAiChatConfigResponse updateConfig(UpdateProductAiChatConfigRequest request) {
        ProductAiChatSettings settings = getOrCreateSettings();
        settings.setChatboxDisplayName(normalizeDisplayName(request.chatboxDisplayName()));
        settings.setMaxQuestionsPerUserPerDay(Math.max(0, request.maxQuestionsPerUserPerDay()));
        ProductAiChatSettings saved = settingsRepository.save(settings);
        return toConfigResponse(saved);
    }

    @Override
    public ProductAiChatOverviewResponse getOverview() {
        ProductAiChatSettings settings = getOrCreateSettings();
        Instant startOfDay = getStartOfDay();

        return new ProductAiChatOverviewResponse(
                messageRepository.countByActiveTrue(),
                messageRepository.countDistinctKnownUsers(),
                messageRepository.countByCreatedAtGreaterThanEqualAndActiveTrue(startOfDay),
                settings.getMaxQuestionsPerUserPerDay()
        );
    }

    @Override
    public List<ProductAiMonthlyQuestionResponse> getMonthlyQuestionStats() {
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        Instant startOfYear = now.withDayOfYear(1).toLocalDate().atStartOfDay(VN_ZONE).toInstant();
        Instant startOfNextYear = now.plusYears(1).withDayOfYear(1).toLocalDate().atStartOfDay(VN_ZONE).toInstant();

        Map<Integer, Long> countByMonth = messageRepository.findMonthlyQuestionStats(startOfYear, startOfNextYear)
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getMonth() == null ? 0 : item.getMonth(),
                        item -> item.getQuestionCount() == null ? 0L : item.getQuestionCount(),
                        Long::sum
                ));

        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new ProductAiMonthlyQuestionResponse(
                        month,
                        countByMonth.getOrDefault(month, 0L)
                ))
                .toList();
    }

    @Override
    public List<ProductAiTopQuestionResponse> getTopQuestions(Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 10 : limit, 50));
        int safeSampleSize = Math.max(safeLimit * 6, 40);
        int maxSampleSize = Math.max(40, topicAnalysisSampleSize);
        safeSampleSize = Math.min(safeSampleSize, maxSampleSize);

        List<ProductAiChatMessageRepository.TopQuestionProjection> rawQuestions = messageRepository.findTopQuestions(
                PageRequest.of(0, safeSampleSize)
        );

        if (rawQuestions.isEmpty()) {
            return List.of();
        }

        if (!StringUtils.hasText(geminiApiKey)) {
            throw new IllegalStateException("Chua cau hinh Gemini API key cho phan tich chu de AI Chat.");
        }

        List<ProductAiTopQuestionResponse> topicStats = groupQuestionsByTopicWithAi(rawQuestions, safeLimit);
        if (!topicStats.isEmpty()) {
            return topicStats;
        }

        throw new IllegalStateException("Khong the phan tich chu de AI Chat tu du lieu cau hoi.");
    }

    @Override
    public PageResponse<ProductAiUserUsageResponse> getUserUsage(
            Pageable pageable,
            String emailKeyword,
            Instant fromTime,
            Instant toTime
    ) {
        ProductAiChatSettings settings = getOrCreateSettings();
        int limit = settings.getMaxQuestionsPerUserPerDay() == null ? 0 : Math.max(0, settings.getMaxQuestionsPerUserPerDay());

        Instant normalizedFromTime = fromTime;
        Instant normalizedToTime = toTime;
        if (normalizedFromTime != null && normalizedToTime != null && normalizedFromTime.isAfter(normalizedToTime)) {
            throw new IllegalArgumentException("Thoi gian bat dau phai nho hon hoac bang thoi gian ket thuc.");
        }

        String normalizedEmailKeyword = StringUtils.hasText(emailKeyword) ? emailKeyword.trim() : null;

        Page<ProductAiChatMessageRepository.UserUsageProjection> page = messageRepository.findUserUsageStats(
                getStartOfDay(),
                normalizedEmailKeyword,
                normalizedFromTime,
                normalizedToTime,
                pageable
        );

        List<@NonNull ProductAiUserUsageResponse> content = page.getContent().stream()
                .map(item -> {
                    Long todayCount = item.getTodayQuestionCount() == null ? 0L : item.getTodayQuestionCount();
                    Integer remaining = limit <= 0 ? null : (int) Math.max(0, limit - todayCount);
                    String keycloakId = StringUtils.hasText(item.getUserKeycloakId()) ? item.getUserKeycloakId() : "anonymous";
                    String email = StringUtils.hasText(item.getUserEmail()) ? item.getUserEmail() : "anonymous";
                    return new ProductAiUserUsageResponse(
                            keycloakId,
                            email,
                            item.getQuestionCount() == null ? 0L : item.getQuestionCount(),
                            todayCount,
                            remaining,
                            item.getLastAskedAt()
                    );
                })
                .toList();

        Page<ProductAiUserUsageResponse> mappedPage = new PageImpl<>(
                content,
                page.getPageable(),
                page.getTotalElements()
        );

        return new PageResponse<>(mappedPage);
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

    private ProductAiChatConfigResponse toConfigResponse(ProductAiChatSettings settings) {
        return new ProductAiChatConfigResponse(
                settings.getChatboxDisplayName(),
                settings.getMaxQuestionsPerUserPerDay()
        );
    }

    private Instant getStartOfDay() {
        return ZonedDateTime.now(VN_ZONE).toLocalDate().atStartOfDay(VN_ZONE).toInstant();
    }

    private String normalizeDisplayName(String value) {
        if (!StringUtils.hasText(value)) {
            return "Tea4Life AI";
        }
        return value.trim();
    }

    private List<ProductAiTopQuestionResponse> groupQuestionsByTopicWithAi(
            List<ProductAiChatMessageRepository.TopQuestionProjection> source,
            int topicLimit
    ) {
        String input = buildTopicAnalysisInput(source);
        if (!StringUtils.hasText(input)) {
            return List.of();
        }

        Map<Integer, String> topicByIndex = askGeminiForTopicMapping(input, topicLimit);
        if (topicByIndex.isEmpty()) {
            return List.of();
        }

        Map<String, Long> grouped = new LinkedHashMap<>();
        for (int i = 0; i < source.size(); i++) {
            ProductAiChatMessageRepository.TopQuestionProjection item = source.get(i);
            int index = i + 1;
            String topic = normalizeTopicLabel(topicByIndex.get(index));
            if (!StringUtils.hasText(topic)) {
                topic = "Khac";
            }

            long count = item.getQuestionCount() == null ? 0L : item.getQuestionCount();
            grouped.merge(topic, count, Long::sum);
        }

        if (grouped.isEmpty()) {
            return List.of();
        }

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(topicLimit)
                .map(entry -> new ProductAiTopQuestionResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String buildTopicAnalysisInput(List<ProductAiChatMessageRepository.TopQuestionProjection> source) {
        int maxInputChars = Math.max(1500, topicAnalysisMaxInputCharacters);
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < source.size(); i++) {
            ProductAiChatMessageRepository.TopQuestionProjection item = source.get(i);
            String question = safeQuestion(item.getQuestion());
            long count = item.getQuestionCount() == null ? 0L : item.getQuestionCount();
            String line = String.format(Locale.ROOT, "%d|count=%d|question=%s%n", i + 1, count, question);

            if (builder.length() + line.length() > maxInputChars) {
                break;
            }
            builder.append(line);
        }

        return builder.toString().trim();
    }

    private Map<Integer, String> askGeminiForTopicMapping(String rawInput, int topicLimit) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text",
                                """
                                Ban la he thong phan tich du lieu cau hoi khach hang.
                                Nhiem vu:
                                - Gom nhom cac cau hoi tuong dong ve CUNG MOT CHU DE.
                                - Moi chu de dat ten ngan gon (toi da 6 tu), ro nghia, tieng Viet khong dau.
                                - Chuan hoa de giam trung lap y nghia.
                                Dau ra DUY NHAT JSON, KHONG markdown:
                                {"items":[{"index":1,"topic":"..."}]}
                                index phai dung voi du lieu dau vao. topic khong de trong.
                                """
                        ))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of(
                                "text",
                                "MAX_TOPICS=" + topicLimit + "\nDATA:\n" + rawInput
                        ))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 1200,
                        "responseMimeType", "application/json"
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

                String text = extractResponseText(rawResponse);
                return parseTopicMappingJson(text);
            } catch (RestClientResponseException ex) {
                int statusCode = ex.getStatusCode().value();
                boolean canRetry = isRetryableStatus(statusCode) && attempt < maxAttempts;
                if (canRetry) {
                    sleepBeforeRetry(backoffMillis);
                    backoffMillis = nextBackoffMillis(backoffMillis);
                    continue;
                }
                log.warn("Khong the gom nhom chu de bang AI (status={}): {}", statusCode, ex.getMessage());
                return Map.of();
            } catch (Exception ex) {
                boolean canRetry = attempt < maxAttempts;
                if (canRetry) {
                    sleepBeforeRetry(backoffMillis);
                    backoffMillis = nextBackoffMillis(backoffMillis);
                    continue;
                }
                log.warn("Khong the gom nhom chu de bang AI: {}", ex.getMessage());
                return Map.of();
            }
        }

        return Map.of();
    }

    private String extractResponseText(String rawResponse) throws Exception {
        if (!StringUtils.hasText(rawResponse)) {
            return "";
        }
        JsonNode response = objectMapper.readTree(rawResponse);
        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
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
        return builder.toString().trim();
    }

    private Map<Integer, String> parseTopicMappingJson(String text) {
        if (!StringUtils.hasText(text)) {
            return Map.of();
        }

        List<String> parseCandidates = new ArrayList<>();
        String stripped = stripCodeFence(text);
        parseCandidates.add(stripped);

        String extractedObject = extractFirstJsonObject(stripped);
        if (StringUtils.hasText(extractedObject)) {
            parseCandidates.add(extractedObject);
        }

        for (String candidate : parseCandidates) {
            try {
                JsonNode root = objectMapper.readTree(candidate);
                JsonNode items = root.path("items");
                if (!items.isArray()) {
                    continue;
                }

                Map<Integer, String> result = new LinkedHashMap<>();
                for (JsonNode item : items) {
                    int index = item.path("index").asInt(-1);
                    String topic = normalizeTopicLabel(item.path("topic").asText(null));
                    if (index > 0 && StringUtils.hasText(topic)) {
                        result.put(index, topic);
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (Exception ignored) {
                // try next candidate
            }
        }

        log.warn("Khong parse duoc ket qua gom nhom chu de: {}", stripped);
        return Map.of();
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        List<String> lines = new ArrayList<>(List.of(trimmed.split("\\R")));
        if (!lines.isEmpty() && lines.get(0).startsWith("```")) {
            lines.remove(0);
        }
        if (!lines.isEmpty() && lines.get(lines.size() - 1).startsWith("```")) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines).trim();
    }

    private String extractFirstJsonObject(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return value.substring(start, end + 1).trim();
    }

    private String normalizeTopicLabel(String topic) {
        if (!StringUtils.hasText(topic)) {
            return null;
        }
        String normalized = topic
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }

    private String safeQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            return "-";
        }
        return question
                .replaceAll("\\s+", " ")
                .replace("|", " ")
                .trim();
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
}
