package com.campusqa.demo.config;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地 Mock ChatModel 配置。
 * 当未启用 ZhipuAI 真正的 ChatModel 时，提供一个可注入的本地兜底实现，
 * 保证系统能够正常启动并返回可读结果。
 */
@Configuration
public class MockChatModelConfig {

    private static final Pattern KNOWLEDGE_BLOCK_PATTERN = Pattern.compile(
            "<<LOCAL_KNOWLEDGE>>(.*?)<</LOCAL_KNOWLEDGE>>",
            Pattern.DOTALL
    );
    private static final int MAX_REPLY_LENGTH = 240;

    /**
     * 标识当前注入的是本地 Mock 模型，方便 Service 层识别本地模式。
     */
    public interface LocalMockChatModel extends ChatModel {
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel mockChatModel() {
        return new LocalMockChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                String knowledge = extractKnowledge(prompt);
                String reply = knowledge.isBlank()
                        ? "[本地模式] 暂未检索到相关校园信息，请换个关键词再试。"
                        : "[本地模式] 检索到信息：" + knowledge;
                return new ChatResponse(List.of(new Generation(new AssistantMessage(reply))));
            }

            @Override
            public ChatOptions getDefaultOptions() {
                return null;
            }
        };
    }

    private String extractKnowledge(Prompt prompt) {
        if (prompt == null) {
            return "";
        }

        String content = prompt.getContents();
        if (content == null || content.isBlank()) {
            return "";
        }

        Matcher matcher = KNOWLEDGE_BLOCK_PATTERN.matcher(content);
        String knowledgeBlock = matcher.find() ? matcher.group(1) : content;

        return normalizeForReply(knowledgeBlock);
    }

    private String normalizeForReply(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = text
                .replace("官方依据汇总：", "")
                .replace("学生经验汇总：", "学生经验：")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.length() <= MAX_REPLY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_REPLY_LENGTH) + "...";
    }
}
