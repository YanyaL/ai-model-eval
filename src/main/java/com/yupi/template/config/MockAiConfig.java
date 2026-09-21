package com.yupi.template.config;

import com.yupi.template.ai.MockChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Mock AI：无真实 Key（或显式 AI_MOCK_ENABLED=true）时启用
 */
@Slf4j
@Configuration
public class MockAiConfig {

    @Bean
    @Primary
    @Conditional(MockAiEnabledCondition.class)
    public ChatModel mockChatModel() {
        log.warn("已注册 MockChatModel（演示模式）");
        return new MockChatModel();
    }
}
