package com.campusqa.demo.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Embedding 模型兜底配置。
 * <p>
 * 切换逻辑：
 * <ol>
 * <li>当环境变量 {@code ZHIPUAI_API_KEY} 已配置且非空时，
 * Spring AI 的 ZhipuAI 自动配置会注册 {@link EmbeddingModel}，
 * 此时本类的 fallback Bean <b>不会</b>被创建。</li>
 * <li>当 {@code ZHIPUAI_API_KEY} 未配置时，
 * 本类提供的本地 ONNX Transformer 模型<b>自动生效</b>。</li>
 * </ol>
 *
 * <b>本地模型</b>：sentence-transformers/all-MiniLM-L6-v2（ONNX 格式，384 维）
 */
@Configuration
public class EmbeddingConfig {

    /**
     * 本地 ONNX Embedding 模型兜底。
     * 仅在没有其他 EmbeddingModel Bean 时创建。
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel fallbackEmbeddingModel() {
        TransformersEmbeddingModel embeddingModel = new TransformersEmbeddingModel();
        embeddingModel.setModelResource(new ClassPathResource("models/all-MiniLM-L6-v2/model.onnx"));
        embeddingModel.setTokenizerResource(new ClassPathResource("models/all-MiniLM-L6-v2/tokenizer.json"));
        embeddingModel.setDisableCaching(false);
        return embeddingModel;
    }
}
