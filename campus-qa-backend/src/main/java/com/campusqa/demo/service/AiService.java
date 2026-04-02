package com.campusqa.demo.service;

import com.campusqa.demo.config.MockChatModelConfig.LocalMockChatModel;
import com.campusqa.demo.dto.AiReplyItem;
import com.campusqa.demo.dto.AiReplyResponse;
import com.campusqa.demo.model.Answer;
import com.campusqa.demo.model.KnowledgeDocument;
import com.campusqa.demo.model.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final QuestionService questionService;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    /**
     * Java 21 虚拟线程执行器。
     * 每个 SSE 流式请求会被分配到一个独立的虚拟线程上执行。
     */
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    // 注入 ChatModel，配合 required = false 和 Mock 机制实现防御性编程
    @Autowired(required = false)
    private ChatModel chatModel;

    public AiService(QuestionService questionService,
                     KnowledgeService knowledgeService,
                     ObjectMapper objectMapper) {
        this.questionService = questionService;
        this.knowledgeService = knowledgeService;
        this.objectMapper = objectMapper;
    }

    public AiReplyResponse ask(String question) {
        log.debug("处理问答请求（虚拟线程={}）: {}", Thread.currentThread().isVirtual(), question);

        try {
            RetrievalResult retrievalResult = retrieveEvidence(question);
            EvidenceBundle evidenceBundle = buildEvidenceBundle(
                    retrievalResult.matchedKnowledge(),
                    retrievalResult.matchedQuestions()
            );
            Prompt prompt = buildPrompt(question, retrievalResult.matchedKnowledge(), evidenceBundle);

            if (chatModel == null || chatModel instanceof LocalMockChatModel) {
                String localAnswer = resolveLocalResponse(prompt, retrievalResult, evidenceBundle);
                return buildResponse(localAnswer, retrievalResult.matchedQuestions(), evidenceBundle, false, "local");
            }

            String answer = extractAnswerText(chatModel.call(prompt));
            if (answer.isBlank()) {
                answer = buildLocalModeAnswer(retrievalResult, evidenceBundle);
            }
            return buildResponse(answer, retrievalResult.matchedQuestions(), evidenceBundle, true, "remote");
        } catch (Exception e) {
            log.error("问答服务处理失败，已切换到友好兜底响应", e);
            return buildResponse(
                    "[本地模式] 当前检索服务有波动，请稍后再试或换个关键词。",
                    List.of(),
                    new EvidenceBundle(List.of(), List.of()),
                    false,
                    "local"
            );
        }
    }

    public SseEmitter streamAsk(String question) {
        SseEmitter emitter = new SseEmitter(0L);

        RetrievalResult retrievalResult = retrieveEvidence(question);
        EvidenceBundle evidenceBundle = buildEvidenceBundle(
                retrievalResult.matchedKnowledge(),
                retrievalResult.matchedQuestions()
        );

        // 在虚拟线程上执行流式推送
        executorService.execute(() -> {
            try {
                sendJsonEvent(emitter, Map.of(
                        "type", "meta",
                        "refs", evidenceBundle.officialRefs(),
                        "officialRefs", evidenceBundle.officialRefs(),
                        "studentExperiences", evidenceBundle.studentExperiences(),
                        "matchedQuestions", toReplyItems(retrievalResult.matchedQuestions())
                ));

                // 防御性编程：空值校验与 Mock 模型识别
                if (chatModel == null || chatModel instanceof LocalMockChatModel) {
                    Prompt prompt = buildPrompt(question, retrievalResult.matchedKnowledge(), evidenceBundle);
                    String localAnswer = resolveLocalResponse(prompt, retrievalResult, evidenceBundle);
                    streamLocalFallback(localAnswer, emitter);
                    return;
                }

                Prompt prompt = buildPrompt(question, retrievalResult.matchedKnowledge(), evidenceBundle);
                Flux<ChatResponse> flux = chatModel.stream(prompt);

                flux.subscribe(
                        response -> {
                            String content = extractAnswerText(response);
                            if (content != null && !content.isBlank()) {
                                try {
                                    sendJsonEvent(emitter, Map.of("type", "delta", "content", content, "remote", true, "mode", "remote"));
                                } catch (IOException ignored) {}
                            }
                        },
                        error -> {
                            log.error("AI 流式输出异常 (可能是未配置或网络错误)", error);
                            try {
                                streamLocalFallback(buildLocalModeAnswer(retrievalResult, evidenceBundle), emitter);
                            } catch (Exception e) {
                                emitter.completeWithError(error);
                            }
                        },
                        () -> {
                            try {
                                sendJsonEvent(emitter, Map.of("type", "done", "remote", true, "mode", "remote"));
                                emitter.complete();
                            } catch (IOException ignored) {}
                        }
                );
            } catch (Exception e) {
                log.error("SSE 推送外层异常", e);
                try {
                    streamLocalFallback(buildLocalModeAnswer(retrievalResult, evidenceBundle), emitter);
                } catch (Exception fallbackException) {
                    emitter.completeWithError(e);
                }
            }
        });

        return emitter;
    }

    private Prompt buildPrompt(String question, List<KnowledgeDocument> matchedKnowledge, EvidenceBundle evidenceBundle) {
        String systemPrompt = """
                你是校园信息问答助手。请严格基于提供的校园知识片段回答，不能编造信息。
                你必须把"官方依据"和"学生经验"严格区分：
                1. "官方依据"只能来自官方知识摘要、参考来源、管理员口径。
                2. "学生经验"只能来自学生回答或同学经验，不能冒充官方规定。
                3. 如果官方依据不足，要明确写"当前知识不足以确认官方结论"。
                4. 如果没有可靠学生经验，要明确写"暂未检索到可靠学生经验"。
                输出格式固定为：
                【直接结论】...
                【官方依据】...
                【学生经验】...
                保持简洁中文，不要出现模型自我介绍。
                """;

        String knowledgeContext = buildKnowledgeContext(matchedKnowledge, evidenceBundle);
        String userPrompt = "用户问题：\n" + question + "\n\n校园知识片段：\n<<LOCAL_KNOWLEDGE>>\n"
                + knowledgeContext
                + "\n<</LOCAL_KNOWLEDGE>>";

        return new Prompt(systemPrompt + "\n\n" + userPrompt);
    }

    private void streamLocalFallback(String text, SseEmitter emitter) throws Exception {
        int step = 10;
        for (int i = 0; i < text.length(); i += step) {
            String part = text.substring(i, Math.min(text.length(), i + step));
            sendJsonEvent(emitter, Map.of("type", "delta", "content", part, "remote", false, "mode", "local"));
            Thread.sleep(35);
        }
        sendJsonEvent(emitter, Map.of("type", "done", "remote", false, "mode", "local"));
        emitter.complete();
    }

    private String buildKnowledgeContext(List<KnowledgeDocument> matchedKnowledge, EvidenceBundle evidenceBundle) {
        if (matchedKnowledge.isEmpty()) {
            return "官方依据：当前没有检索到相关校园知识。\n学生经验：暂未检索到可靠学生经验。";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("官方依据汇总：\n");
        for (int i = 0; i < matchedKnowledge.size(); i++) {
            KnowledgeDocument item = matchedKnowledge.get(i);
            builder.append(i + 1).append(". 标题：").append(item.getTitle()).append("\n")
                    .append("分类：").append(item.getCategory()).append("\n")
                    .append("部门：").append(item.getDepartment()).append("\n")
                    .append("校区：").append(item.getCampus()).append("\n")
                    .append("发布时间：").append(item.getPublishDate()).append("\n");
            if (item.getSummary() != null && !item.getSummary().isBlank()) {
                builder.append("官方知识摘要：").append(item.getSummary()).append("\n");
            }
            if (item.getContent() != null && !item.getContent().isBlank()) {
                builder.append("详细说明：").append(item.getContent()).append("\n");
            }
            builder.append("官方参考来源：").append(item.getSourceName()).append("\n\n");
        }

        builder.append("学生经验汇总：\n");
        if (evidenceBundle.studentExperiences().isEmpty()) {
            builder.append("暂未检索到可靠学生经验。\n");
        } else {
            for (String item : evidenceBundle.studentExperiences()) {
                builder.append("- ").append(item).append("\n");
            }
        }
        return builder.toString();
    }

    private RetrievalResult retrieveEvidence(String question) {
        List<String> warnings = new ArrayList<>();
        List<KnowledgeDocument> matchedKnowledge = safeFindRelevantKnowledge(question, warnings);
        List<Question> matchedQuestions = safeFindRelevantQuestions(question, warnings);
        return new RetrievalResult(matchedKnowledge, matchedQuestions, warnings);
    }

    private List<KnowledgeDocument> safeFindRelevantKnowledge(String question, List<String> warnings) {
        try {
            return knowledgeService.findRelevantKnowledge(question, 3);
        } catch (Exception exception) {
            log.error("官方知识检索失败，已降级为空结果", exception);
            warnings.add("官方知识检索暂时不可用");
            return List.of();
        }
    }

    private List<Question> safeFindRelevantQuestions(String question, List<String> warnings) {
        try {
            return questionService.findRelevantQuestions(question, 3);
        } catch (Exception exception) {
            log.error("学生问答检索失败，已降级为空结果", exception);
            warnings.add("学生经验检索暂时不可用");
            return List.of();
        }
    }

    private String resolveLocalResponse(Prompt prompt,
                                        RetrievalResult retrievalResult,
                                        EvidenceBundle evidenceBundle) {
        if (chatModel != null) {
            try {
                String answer = extractAnswerText(chatModel.call(prompt));
                if (!answer.isBlank()) {
                    return answer;
                }
            } catch (Exception exception) {
                log.warn("本地 Mock ChatModel 调用失败，使用 Service 层兜底文案", exception);
            }
        }
        return buildLocalModeAnswer(retrievalResult, evidenceBundle);
    }

    private String buildLocalModeAnswer(RetrievalResult retrievalResult, EvidenceBundle evidenceBundle) {
        List<String> segments = new ArrayList<>();
        if (!retrievalResult.retrievalWarnings().isEmpty()) {
            segments.add("检索过程中出现波动，系统已自动降级。");
        }

        if (!retrievalResult.matchedKnowledge().isEmpty()) {
            KnowledgeDocument topKnowledge = retrievalResult.matchedKnowledge().getFirst();
            String mainContent = firstNonBlank(
                    topKnowledge.getSummary(),
                    topKnowledge.getContent(),
                    topKnowledge.getTitle()
            );
            segments.add("检索到信息：" + mainContent);
            if (topKnowledge.getSourceName() != null && !topKnowledge.getSourceName().isBlank()) {
                segments.add("来源：" + topKnowledge.getSourceName());
            }
        } else if (!evidenceBundle.studentExperiences().isEmpty()) {
            segments.add("检索到同学经验：" + evidenceBundle.studentExperiences().getFirst());
        } else {
            segments.add("暂未检索到相关校园信息，请换个关键词再试。");
        }

        return "[本地模式] " + String.join(" ", segments);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "暂无可展示内容。";
    }

    private AiReplyResponse buildResponse(String answer,
                                          List<Question> matchedQuestions,
                                          EvidenceBundle evidenceBundle,
                                          boolean remote,
                                          String mode) {
        AiReplyResponse result = new AiReplyResponse();
        result.setAnswer(answer);
        result.setRefs(evidenceBundle.officialRefs());
        result.setOfficialRefs(evidenceBundle.officialRefs());
        result.setStudentExperiences(evidenceBundle.studentExperiences());
        result.setMatchedQuestions(toReplyItems(matchedQuestions));
        result.setRemote(remote);
        result.setMode(mode);
        return result;
    }

    private EvidenceBundle buildEvidenceBundle(List<KnowledgeDocument> matchedKnowledge,
                                               List<Question> matchedQuestions) {
        return new EvidenceBundle(buildOfficialRefs(matchedKnowledge), buildStudentExperiences(matchedQuestions));
    }

    private List<String> buildOfficialRefs(List<KnowledgeDocument> matchedKnowledge) {
        LinkedHashSet<String> refs = new LinkedHashSet<>();
        for (KnowledgeDocument item : matchedKnowledge) {
            if (item.getSourceName() != null && !item.getSourceName().isBlank()) {
                refs.add(item.getSourceName());
            }
            if (item.getTitle() != null && !item.getTitle().isBlank()) {
                refs.add(item.getTitle());
            }
        }
        return refs.stream().limit(4).toList();
    }

    private List<String> buildStudentExperiences(List<Question> matchedQuestions) {
        LinkedHashSet<String> experiences = new LinkedHashSet<>();
        for (Question item : matchedQuestions) {
            if (item.getAnswers() == null || item.getAnswers().isEmpty()) {
                continue;
            }
            for (Answer answer : item.getAnswers()) {
                if (answer.getContent() == null || answer.getContent().isBlank()) {
                    continue;
                }
                String prefix = answer.getAuthorName() == null || answer.getAuthorName().isBlank()
                        ? "同学经验："
                        : answer.getAuthorName() + "：";
                experiences.add(prefix + answer.getContent().trim());
                if (experiences.size() >= 3) {
                    return new ArrayList<>(experiences);
                }
            }
        }
        return new ArrayList<>(experiences);
    }

    private List<AiReplyItem> toReplyItems(List<Question> matchedQuestions) {
        return matchedQuestions.stream()
                .map(item -> new AiReplyItem(item.getId(), item.getTitle(), item.getCategory()))
                .toList();
    }

    private void sendJsonEvent(SseEmitter emitter, Map<String, Object> payload) throws IOException {
        String json = Objects.requireNonNull(objectMapper.writeValueAsString(payload), "JSON result cannot be null");
        emitter.send(SseEmitter.event().data(json));
    }

    private String extractAnswerText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text.trim();
    }

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
        log.info("AI SSE 执行器已关闭");
    }

    private record EvidenceBundle(List<String> officialRefs, List<String> studentExperiences) {}

    private record RetrievalResult(List<KnowledgeDocument> matchedKnowledge,
                                   List<Question> matchedQuestions,
                                   List<String> retrievalWarnings) {}
}
