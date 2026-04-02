package com.campusqa.demo.config;

import org.springframework.ai.autoconfigure.zhipuai.ZhiPuAiAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * 智谱 AI 配置（仅在 zhipuai profile 激活时生效）。
 * <p>
 * 使用方法：
 * <pre>
 *   # 1. 设置环境变量
 *   export ZHIPUAI_API_KEY=your_zhipuai_api_key
 *
 *   # 2. 启动时激活 profile
 *   mvn spring-boot:run -Dspring-boot.run.profiles=zhipuai
 * </pre>
 * <p>
 * 激活后，ZhipuAI 会提供更高质量的中文 Embedding 模型（embedding-3），
 * 本地 ONNX 模型会自动让位（{@code @ConditionalOnMissingBean}）。
 */
@Configuration
@Profile("zhipuai")
@Import(ZhiPuAiAutoConfiguration.class)
public class ZhiPuAiProfileConfig {
    // 空类体：仅通过 @Import 在 zhipuai profile 下重新引入被排除的自动配置
}
