package com.yupi.template.service;

import com.yupi.template.ai.AiKeyUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 当前 AI 运行模式（Mock / 真实 OpenRouter）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModeService {

    private final Environment environment;

    @PostConstruct
    public void logModeOnStartup() {
        if (isMockEnabled()) {
            log.warn("AI 模式 = MOCK：不会调用真实大模型。在 .env 填入真实 OPENROUTER_API_KEY，并去掉 AI_MOCK_ENABLED（或设为 false）即可切换。");
        } else {
            log.info("AI 模式 = LIVE：将通过 OpenRouter 调用真实大模型。");
        }
    }

    public boolean isMockEnabled() {
        return AiKeyUtils.shouldUseMock(environment);
    }

    public boolean hasRealApiKey() {
        return AiKeyUtils.isRealOpenRouterKey(AiKeyUtils.resolveApiKey(environment));
    }

    public Map<String, Object> status() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mockEnabled", isMockEnabled());
        map.put("hasRealApiKey", hasRealApiKey());
        map.put("mode", isMockEnabled() ? "mock" : "live");
        map.put("hint", isMockEnabled()
                ? "Fill OPENROUTER_API_KEY (sk-or-...) and set AI_MOCK_ENABLED=false (or leave AI_MOCK_ENABLED empty) to use live models."
                : "Live OpenRouter mode is active.");
        return map;
    }

    /**
     * 真实模式但无有效 Key 时抛错，避免静默失败
     */
    public void requireLiveApiKeyOrThrow() {
        if (isMockEnabled()) {
            return;
        }
        if (!hasRealApiKey()) {
            throw new com.yupi.template.exception.BusinessException(
                    com.yupi.template.exception.ErrorCode.SYSTEM_ERROR,
                    "已关闭 Mock，但未配置有效的 OPENROUTER_API_KEY（需 sk-or- 开头）。请检查 .env。"
            );
        }
    }
}
