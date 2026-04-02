package com.campusqa.demo.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置。
 * <p>
 * 创建两个独立的 {@link SimpleVectorStore} 实例：
 * <ul>
 *   <li><b>knowledgeVectorStore</b> – 官方知识文档向量索引</li>
 *   <li><b>questionVectorStore</b> – 学生问答向量索引</li>
 * </ul>
 * 底层的 {@link EmbeddingModel} 由
 * {@link EmbeddingConfig} 根据环境变量自动选择（ZhipuAI 或本地 ONNX）。
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public SimpleVectorStore knowledgeVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public SimpleVectorStore questionVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
