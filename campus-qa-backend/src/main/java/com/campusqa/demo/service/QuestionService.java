package com.campusqa.demo.service;

import com.campusqa.demo.dto.CreateQuestionRequest;
import com.campusqa.demo.exception.NotFoundException;
import com.campusqa.demo.model.AiAnswer;
import com.campusqa.demo.model.Answer;
import com.campusqa.demo.model.Question;
import com.campusqa.demo.support.VectorStorePersistenceSupport;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.nio.file.Path;

/**
 * 学生问答服务。
 * <p>
 * 核心检索已从朴素的 {@code String.contains()} 字符串匹配
 * 升级为基于 Spring AI {@link SimpleVectorStore} 的向量语义检索。
 */
@Service
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Path STORE_FILE = VectorStorePersistenceSupport.resolveStoreFile("question-vector-store.json");
    private static final Path SIGNATURE_FILE = VectorStorePersistenceSupport.resolveSignatureFile("question-vector-store.json");

    /** 向量检索的相似度阈值 */
    private static final double SIMILARITY_THRESHOLD = 0.7;

    private final SimpleVectorStore questionVectorStore;

    private final List<Question> questions = new CopyOnWriteArrayList<>();
    private final Map<String, Question> questionIndex = new ConcurrentHashMap<>();

    private final List<String> categories = List.of("食堂餐饮", "图书馆", "快递服务", "教学办事", "宿舍生活", "校园常见问题");
    private final List<String> tags = List.of("营业时间", "位置导航", "借阅规则", "失物招领", "空调维修", "快递代取", "缴费办理", "自习室");

    public QuestionService(@Qualifier("questionVectorStore") SimpleVectorStore questionVectorStore) {
        this.questionVectorStore = questionVectorStore;
    }

    @PostConstruct
    public void init() {
        questions.clear();
        questionIndex.clear();

        questions.add(seedQuestion(
                "q1001",
                "图书馆晚上几点闭馆？",
                "想问一下工作日图书馆一楼自习区最晚开放到几点，周末会不会提前闭馆？",
                "图书馆",
                List.of("借阅规则", "自习室"),
                "20220001",
                "张同学",
                "2026-03-20 18:20",
                126,
                true,
                "根据图书馆公告，工作日一楼自习区通常开放至 22:30，周末为 22:00，节假日安排以馆内通知为准。",
                List.of("图书馆开放时间通知", "2026 春季学期自习区安排"),
                List.of(new Answer("a9001", "图书馆管理员", "工作日一楼自习区到 22:30，周末到 22:00，如果遇到考试周可能会延长。", true, "2026-03-20 18:40"))
        ));
        questions.add(seedQuestion(
                "q1002",
                "南苑食堂早餐推荐窗口有哪些？",
                "第一次去南苑食堂，想知道早餐哪几个窗口排队比较少，豆浆和包子在哪里买。",
                "食堂餐饮",
                List.of("营业时间"),
                "20220002",
                "李同学",
                "2026-03-21 08:10",
                89,
                true,
                "南苑食堂 1 楼靠东侧窗口早餐高峰相对较快，豆浆和包子通常在 1 号和 3 号窗口供应。",
                List.of("南苑食堂窗口分布", "后勤早餐供应安排"),
                List.of(new Answer("a9002", "王同学", "1 楼 1 号窗口最稳，包子和豆浆都有，7:30 以后人会多起来。", false, "2026-03-21 08:25"))
        ));

        // 构建索引并写入向量库
        List<Document> aiDocuments = new ArrayList<>();
        for (Question q : questions) {
            questionIndex.put(q.getId(), q);
            aiDocuments.add(toAiDocument(q));
        }
        if (!aiDocuments.isEmpty()) {
            String signature = VectorStorePersistenceSupport.calculateSignature(aiDocuments);
            if (tryLoadPersistedStore(signature)) {
                log.info("问答向量库已从本地索引加载，共载入 {} 条问题", aiDocuments.size());
                return;
            }
            try {
                questionVectorStore.add(aiDocuments);
                persistStore(signature);
                log.info("问答向量库初始化完成，共写入 {} 条问题", aiDocuments.size());
            } catch (Exception exception) {
                log.warn("问答向量库初始化失败，系统将自动降级为关键词检索", exception);
            }
        }
    }

    // ── 公共接口 ──────────────────────────────────────────

    public List<Question> list(String keyword, String category) {
        String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
        return questions.stream()
                .filter(question -> matchesKeyword(question, normalizedKeyword))
                .filter(question -> matchesCategory(question, category))
                .sorted(Comparator.comparing(this::parseCreatedAtSafely).reversed())
                .toList();
    }

    public Question getById(String id) {
        return questions.stream()
                .filter(question -> question.getId().equals(id))
                .findFirst()
                .map(this::increaseView)
                .orElseThrow(() -> new NotFoundException("问题不存在"));
    }

    public Question create(CreateQuestionRequest request) {
        Question question = new Question();
        question.setId("q" + System.currentTimeMillis());
        question.setTitle(request.getTitle().trim());
        question.setContent(request.getContent().trim());
        question.setCategory(request.getCategory().trim());
        question.setTags(normalizeTags(request.getTags()));
        question.setAuthor(request.getAuthor().trim());
        question.setAuthorName(request.getAuthorName().trim());
        question.setCreatedAt(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        question.setViews(0);
        question.setHot(false);
        question.setAiAnswer(new AiAnswer(
                "当前为基础版接口演示，后续这里会接入知识检索与大模型，根据校园知识库生成回答。",
                List.of("待接入知识条目", "待接入大模型")
        ));
        question.setAnswers(new ArrayList<>());
        questions.add(0, question);

        // 新创建的问题同步写入向量库
        questionIndex.put(question.getId(), question);
        addToVectorStore(question);
        persistStore();

        return question;
    }

    public void delete(String id, String author) {
        Question target = questions.stream()
                .filter(question -> question.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("问题不存在"));

        if (author != null && !author.isBlank() && !Objects.equals(target.getAuthor(), author.trim())) {
            throw new IllegalArgumentException("只能删除自己的提问");
        }

        questions.remove(target);
        questionIndex.remove(id);
        removeFromVectorStore(id);
        persistStore();
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<String> getTags() {
        return tags;
    }

    /**
     * 基于向量语义相似度检索相关问题。
     * <p>
     * 使用 {@link SimpleVectorStore#similaritySearch} 进行余弦相似度匹配，
     * 相似度阈值为 {@value #SIMILARITY_THRESHOLD}。
     * 当向量检索无结果时，自动降级为关键词匹配兜底。
     *
     * @param keyword 用户问题或搜索关键词
     * @param limit   最多返回条数
     * @return 按相似度降序排列的问题列表
     */
    public List<Question> findRelevantQuestions(String keyword, int limit) {
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

            List<Document> results = questionVectorStore.similaritySearch(searchRequest);

            List<Question> matched = results.stream()
                    .map(doc -> questionIndex.get(doc.getId()))
                    .filter(Objects::nonNull)
                    .toList();

            if (!matched.isEmpty()) {
                log.debug("向量检索命中 {} 条问题（query={}）", matched.size(), keyword);
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
     * 将新问题写入向量库。
     */
    private void addToVectorStore(Question question) {
        try {
            questionVectorStore.add(List.of(toAiDocument(question)));
            log.debug("新问题已写入向量库：{}", question.getId());
        } catch (Exception ex) {
            log.warn("写入向量库失败（questionId={}）", question.getId(), ex);
        }
    }

    private void removeFromVectorStore(String questionId) {
        try {
            questionVectorStore.delete(List.of(questionId));
            log.debug("问题已从向量库移除：{}", questionId);
        } catch (Exception ex) {
            log.warn("从向量库删除失败（questionId={}）", questionId, ex);
        }
    }

    /**
     * 将 Question 转化为 Spring AI Document。
     */
    private Document toAiDocument(Question question) {
        String textForEmbedding = String.join(" ",
                valueOrEmpty(question.getTitle()),
                valueOrEmpty(question.getContent()),
                valueOrEmpty(question.getCategory()),
                joinValues(question.getTags()),
                question.getAiAnswer() == null ? "" : valueOrEmpty(question.getAiAnswer().getSummary()));

        return new Document(question.getId(), textForEmbedding,
                Map.of("questionId", question.getId(),
                        "category", valueOrEmpty(question.getCategory())));
    }

    /**
     * 降级的关键词搜索，保留原始 computeScore 逻辑作为兜底。
     */
    private List<Question> fallbackKeywordSearch(String keyword, int limit) {
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT).trim();
        return questions.stream()
                .map(question -> new ScoredQuestion(question, computeScore(question, normalizedKeyword)))
                .filter(item -> item.score > 0)
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(limit)
                .map(item -> item.question)
                .toList();
    }

    private boolean matchesKeyword(Question question, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String searchable = String.join(" ",
                        valueOrEmpty(question.getTitle()),
                        valueOrEmpty(question.getContent()),
                        valueOrEmpty(question.getCategory()),
                        joinValues(question.getTags()))
                .toLowerCase(Locale.ROOT);
        return searchable.contains(keyword);
    }

    private boolean matchesCategory(Question question, String category) {
        return category == null || category.isBlank() || "全部".equals(category) || question.getCategory().equals(category);
    }

    private int computeScore(Question question, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return 0;
        }
        String searchable = String.join(" ",
                        valueOrEmpty(question.getTitle()),
                        valueOrEmpty(question.getContent()),
                        valueOrEmpty(question.getCategory()),
                        joinValues(question.getTags()),
                        question.getAiAnswer() == null ? "" : valueOrEmpty(question.getAiAnswer().getSummary()))
                .toLowerCase(Locale.ROOT);

        int score = 0;
        if (searchable.contains(keyword)) {
            score += 6;
        }

        for (String token : keyword.split("\\s+")) {
            if (!token.isBlank() && searchable.contains(token)) {
                score += 2;
            }
        }

        if (question.isHot()) {
            score += 1;
        }
        return score;
    }

    private Question increaseView(Question question) {
        question.setViews(question.getViews() + 1);
        return question;
    }

    private Question seedQuestion(String id,
                                  String title,
                                  String content,
                                  String category,
                                  List<String> seedTags,
                                  String author,
                                  String authorName,
                                  String createdAt,
                                  int views,
                                  boolean hot,
                                  String aiSummary,
                                  List<String> refs,
                                  List<Answer> answers) {
        Question question = new Question();
        question.setId(id);
        question.setTitle(title);
        question.setContent(content);
        question.setCategory(category);
        question.setTags(new ArrayList<>(seedTags));
        question.setAuthor(author);
        question.setAuthorName(authorName);
        question.setCreatedAt(createdAt);
        question.setViews(views);
        question.setHot(hot);
        question.setAiAnswer(new AiAnswer(aiSummary, refs));
        question.setAnswers(new ArrayList<>(answers));
        return question;
    }

    private List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return new ArrayList<>();
        }
        return rawTags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .limit(6)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private LocalDateTime parseCreatedAtSafely(Question question) {
        try {
            return LocalDateTime.parse(question.getCreatedAt(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            return LocalDateTime.MIN;
        }
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
            questionVectorStore.load(STORE_FILE.toFile());
            return true;
        } catch (Exception exception) {
            log.warn("读取问答向量索引失败，将自动重新构建", exception);
            return false;
        }
    }

    private void persistStore() {
        List<Document> documents = questions.stream()
                .map(this::toAiDocument)
                .toList();
        persistStore(VectorStorePersistenceSupport.calculateSignature(documents));
    }

    private void persistStore(String signature) {
        try {
            VectorStorePersistenceSupport.ensureBaseDirectory();
            questionVectorStore.save(STORE_FILE.toFile());
            VectorStorePersistenceSupport.writeSignature(SIGNATURE_FILE, signature);
        } catch (Exception exception) {
            log.warn("保存问答向量索引失败，下次启动将重新构建", exception);
        }
    }

    private record ScoredQuestion(Question question, int score) {
    }
}
