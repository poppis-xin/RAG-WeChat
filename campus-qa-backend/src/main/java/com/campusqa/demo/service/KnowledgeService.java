package com.campusqa.demo.service;

import com.campusqa.demo.exception.NotFoundException;
import com.campusqa.demo.model.KnowledgeDocument;
import com.campusqa.demo.support.VectorStorePersistenceSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 官方知识文档服务。
 * <p>
 * 核心检索已从朴素的 {@code String.contains()} 字符串匹配
 * 升级为基于 Spring AI {@link SimpleVectorStore} 的向量语义检索。
 * <p>
 * 初始化时将 {@code knowledge-data.json} 中的每一条文档向量化写入
 * {@code knowledgeVectorStore}，后续的 {@link #findRelevantKnowledge}
 * 通过余弦相似度进行语义搜索。
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final Path STORE_FILE = VectorStorePersistenceSupport.resolveStoreFile("knowledge-vector-store.json");
    private static final Path SIGNATURE_FILE = VectorStorePersistenceSupport.resolveSignatureFile("knowledge-vector-store.json");

    /** 向量检索的相似度阈值 */
    private static final double SIMILARITY_THRESHOLD = 0.7;

    private final ObjectMapper objectMapper;
    private final SimpleVectorStore knowledgeVectorStore;

    /** 仍保留完整内存列表，用于 list() 列表展示和 getById() */
    private final List<KnowledgeDocument> documents = new CopyOnWriteArrayList<>();

    /** docId → KnowledgeDocument 快速索引，供向量检索结果反查 */
    private final Map<String, KnowledgeDocument> documentIndex = new ConcurrentHashMap<>();

    public KnowledgeService(ObjectMapper objectMapper,
                            @Qualifier("knowledgeVectorStore") SimpleVectorStore knowledgeVectorStore) {
        this.objectMapper = objectMapper;
        this.knowledgeVectorStore = knowledgeVectorStore;
    }

    @PostConstruct
    public void init() {
        documents.clear();
        documentIndex.clear();
        List<KnowledgeDocument> loaded = loadKnowledgeDocuments();
        documents.addAll(loaded);

        // 构建 docId 索引
        for (KnowledgeDocument doc : loaded) {
            documentIndex.put(doc.getId(), doc);
        }

        // 将所有文档向量化写入 SimpleVectorStore
        List<Document> aiDocuments = loaded.stream()
                .map(this::toAiDocument)
                .toList();

        if (!aiDocuments.isEmpty()) {
            String signature = VectorStorePersistenceSupport.calculateSignature(aiDocuments);
            if (tryLoadPersistedStore(signature)) {
                log.info("知识向量库已从本地索引加载，共载入 {} 条文档", aiDocuments.size());
                return;
            }
            try {
                knowledgeVectorStore.add(aiDocuments);
                persistStore(signature);
                log.info("知识向量库初始化完成，共写入 {} 条文档", aiDocuments.size());
            } catch (Exception exception) {
                log.warn("知识向量库初始化失败，系统将自动降级为关键词检索", exception);
            }
        }
    }

    // ── 公共接口 ──────────────────────────────────────────

    public List<KnowledgeDocument> list(String keyword, String category) {
        String normalizedKeyword = normalize(keyword);
        return documents.stream()
                .filter(item -> matchesKeyword(item, normalizedKeyword))
                .filter(item -> matchesCategory(item, category))
                .sorted(Comparator.comparing(this::parsePublishDateSafely).reversed())
                .toList();
    }

    public KnowledgeDocument getById(String id) {
        return documents.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("官方知识不存在"));
    }

    /**
     * 基于向量语义相似度检索相关知识文档。
     * <p>
     * 使用 {@link SimpleVectorStore#similaritySearch} 进行余弦相似度匹配，
     * 相似度阈值为 {@value #SIMILARITY_THRESHOLD}。
     * 当向量检索无结果时，自动降级为关键词匹配兜底。
     *
     * @param keyword 用户问题或搜索关键词
     * @param limit   最多返回条数
     * @return 按相似度降序排列的知识文档列表
     */
    public List<KnowledgeDocument> findRelevantKnowledge(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        // ── 主路径：向量语义检索 ──
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(keyword)
                    .topK(limit)
                    .similarityThreshold(SIMILARITY_THRESHOLD)
                    .build();

            List<Document> results = knowledgeVectorStore.similaritySearch(searchRequest);

            List<KnowledgeDocument> matched = results.stream()
                    .map(doc -> documentIndex.get(doc.getId()))
                    .filter(java.util.Objects::nonNull)
                    .toList();

            if (!matched.isEmpty()) {
                log.debug("向量检索命中 {} 条知识文档（query={}）", matched.size(), keyword);
                return matched;
            }
        } catch (Exception ex) {
            log.warn("向量检索异常，降级为关键词匹配（query={}）", keyword, ex);
        }

        // ── 降级路径：关键词字符串匹配（兜底） ──
        log.debug("向量检索无结果，降级为关键词匹配（query={}）", keyword);
        return fallbackKeywordSearch(keyword, limit);
    }

    // ── 私有方法 ──────────────────────────────────────────

    /**
     * 降级的关键词搜索，保留原始 computeScore 逻辑作为兜底。
     */
    private List<KnowledgeDocument> fallbackKeywordSearch(String keyword, int limit) {
        String normalizedKeyword = normalize(keyword);
        return documents.stream()
                .map(item -> new ScoredKnowledge(item, computeScore(item, normalizedKeyword)))
                .filter(item -> item.score > 0)
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(limit)
                .map(item -> item.document)
                .toList();
    }

    /**
     * 将 KnowledgeDocument 转化为 Spring AI Document，
     * 拼接核心文本字段作为 Embedding 内容。
     */
    private Document toAiDocument(KnowledgeDocument doc) {
        String textForEmbedding = String.join(" ",
                valueOrEmpty(doc.getTitle()),
                valueOrEmpty(doc.getCategory()),
                valueOrEmpty(doc.getDepartment()),
                valueOrEmpty(doc.getSummary()),
                valueOrEmpty(doc.getContent()),
                valueOrEmpty(doc.getSourceName()),
                valueOrEmpty(doc.getCampus()),
                joinValues(doc.getTags()));

        return new Document(doc.getId(), textForEmbedding,
                Map.of("docId", doc.getId(),
                        "category", valueOrEmpty(doc.getCategory()),
                        "title", valueOrEmpty(doc.getTitle())));
    }

    private List<KnowledgeDocument> loadKnowledgeDocuments() {
        ClassPathResource resource = new ClassPathResource("knowledge-data.json");
        if (!resource.exists()) {
            return new ArrayList<>();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<KnowledgeDocument>>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("加载官方知识库失败", exception);
        }
    }

    private boolean matchesKeyword(KnowledgeDocument document, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return buildCorpus(document).contains(keyword);
    }

    private boolean matchesCategory(KnowledgeDocument document, String category) {
        return category == null || category.isBlank() || "全部".equals(category) || document.getCategory().equals(category);
    }

    private int computeScore(KnowledgeDocument document, String keyword) {
        if (keyword.isBlank()) {
            return 0;
        }

        String corpus = buildCorpus(document);
        int score = 0;
        if (corpus.contains(keyword)) {
            score += 8;
        }

        for (String token : keyword.split("\\s+")) {
            if (!token.isBlank() && corpus.contains(token)) {
                score += 2;
            }
        }

        if (document.isOfficial()) {
            score += 2;
        }
        return score;
    }

    private String buildCorpus(KnowledgeDocument document) {
        return String.join(" ",
                        valueOrEmpty(document.getTitle()),
                        valueOrEmpty(document.getCategory()),
                        valueOrEmpty(document.getDepartment()),
                        valueOrEmpty(document.getSummary()),
                        valueOrEmpty(document.getContent()),
                        valueOrEmpty(document.getSourceName()),
                        valueOrEmpty(document.getCampus()),
                        joinValues(document.getTags()))
                .toLowerCase(Locale.ROOT);
    }

    private LocalDate parsePublishDateSafely(KnowledgeDocument document) {
        try {
            return LocalDate.parse(document.getPublishDate());
        } catch (DateTimeParseException exception) {
            return LocalDate.MIN;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String joinValues(List<String> values) {
        return values == null ? "" : String.join(" ", values);
    }

    private boolean tryLoadPersistedStore(String signature) {
        try {
            if (!STORE_FILE.toFile().exists()) {
                return false;
            }
            if (!VectorStorePersistenceSupport.signatureMatches(SIGNATURE_FILE, signature)) {
                return false;
            }
            knowledgeVectorStore.load(STORE_FILE.toFile());
            return true;
        } catch (Exception exception) {
            log.warn("读取知识向量索引失败，将自动重新构建", exception);
            return false;
        }
    }

    private void persistStore(String signature) {
        try {
            VectorStorePersistenceSupport.ensureBaseDirectory();
            knowledgeVectorStore.save(STORE_FILE.toFile());
            VectorStorePersistenceSupport.writeSignature(SIGNATURE_FILE, signature);
        } catch (Exception exception) {
            log.warn("保存知识向量索引失败，下次启动将重新构建", exception);
        }
    }

    private record ScoredKnowledge(KnowledgeDocument document, int score) {
    }
}
