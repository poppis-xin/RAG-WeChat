package com.campusqa.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园信息问答系统后端入口。
 * <p>
 * 排除 ZhipuAI 自动配置：当 ZHIPUAI_API_KEY 未设置时，ZhipuAI starter 会因缺少
 * key 而启动失败。通过排除自动配置 + 在 application.yml 中使用 profile 实现
 * "有 key 就用 ZhipuAI，没有就用本地 ONNX" 的切换。
 * <p>
 * 如果要启用 ZhipuAI，设置环境变量 ZHIPUAI_API_KEY 后，
 * 启动时激活 profile: {@code --spring.profiles.active=zhipuai}
 */
@SpringBootApplication(exclude = {
        org.springframework.ai.autoconfigure.zhipuai.ZhiPuAiAutoConfiguration.class
})
public class CampusQaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusQaApplication.class, args);
    }
}
