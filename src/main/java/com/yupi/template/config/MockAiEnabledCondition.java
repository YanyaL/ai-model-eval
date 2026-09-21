package com.yupi.template.config;

import com.yupi.template.ai.AiKeyUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 是否启用 Mock ChatModel
 */
public class MockAiEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return AiKeyUtils.shouldUseMock(context.getEnvironment());
    }
}
