package com.yupi.template.ai;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * OpenRouter Key / Mock 模式判定
 */
public final class AiKeyUtils {

    private AiKeyUtils() {
    }

    public static boolean isRealOpenRouterKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String k = key.trim();
        String lower = k.toLowerCase();
        if (lower.contains("mock") || lower.contains("your-key") || lower.contains("xxxxx") || lower.contains("placeholder")) {
            return false;
        }
        return k.startsWith("sk-or-");
    }

    /**
     * AI_MOCK_ENABLED / app.ai.mock-enabled 显式优先；
     * 未设置时：有真实 Key → 真实模型，否则 Mock。
     */
    public static boolean shouldUseMock(Environment env) {
        String explicit = firstNonBlank(
                env.getProperty("AI_MOCK_ENABLED"),
                env.getProperty("app.ai.mock-enabled")
        );
        if (StringUtils.hasText(explicit)) {
            return Boolean.parseBoolean(explicit.trim());
        }
        return !isRealOpenRouterKey(resolveApiKey(env));
    }

    public static String resolveApiKey(Environment env) {
        return firstNonBlank(
                env.getProperty("OPENROUTER_API_KEY"),
                env.getProperty("SPRING_AI_OPENAI_API_KEY"),
                env.getProperty("spring.ai.openai.api-key")
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
