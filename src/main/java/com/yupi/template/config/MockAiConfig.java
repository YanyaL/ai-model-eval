package com.yupi.template.config;

import com.yupi.template.ai.MockChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Mock AI：默认开启，便于在无 OpenRouter Key 时跑通全站
 */
@Slf4j
@Configuration
public class MockAiConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "app.ai", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
    public ChatModel mockChatModel() {
        log.warn("AI Mock 模式已开启：不会调用真实大模型。配置 OPENROUTER_API_KEY 后设置 AI_MOCK_ENABLED=false 可关闭。");
        return new MockChatModel();
    }
}
