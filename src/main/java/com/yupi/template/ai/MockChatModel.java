package com.yupi.template.ai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 无 API Key 时的模拟 ChatModel：支持流式对比与结构化评分/优化 JSON。
 */
public class MockChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        String text = buildResponseText(prompt);
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        String full = buildStreamText(prompt);
        List<String> chunks = chunkText(full, 8);
        return Flux.fromIterable(chunks)
                .delayElements(Duration.ofMillis(25))
                .map(chunk -> new ChatResponse(List.of(new Generation(new AssistantMessage(chunk)))));
    }

    private String buildResponseText(Prompt prompt) {
        String userText = extractUserText(prompt);
        if (userText.contains("total_score") || userText.contains("评分") || userText.contains("evaluation")) {
            return """
                    {"scores":{"accuracy":8,"completeness":8,"clarity":8,"relevance":8},"total_score":8,"rating":4,"comment":"[Mock] 演示评分，未调用真实大模型"}
                    """;
        }
        if (userText.contains("optimized_prompt") || userText.contains("issues") || userText.contains("提示词")) {
            return """
                    {"issues":["表述偏笼统","缺少输出格式约束","未给出角色与边界"],"optimized_prompt":"[Mock 优化] 请你作为资深助手，针对用户目标给出分步骤、可执行的回答，并明确假设与限制。","improvements":["目标更清晰","增加格式约束","补充角色设定"]}
                    """;
        }
        return "[Mock] " + summarize(userText);
    }

    private String buildStreamText(Prompt prompt) {
        String userText = extractUserText(prompt);
        String modelHint = "";
        if (prompt.getOptions() != null) {
            modelHint = String.valueOf(prompt.getOptions());
        }
        return "【演示模式 / Mock Mode】当前未配置大模型 API Key，以下为模拟回答，用于联调前端与业务流程。\n\n"
                + "你的问题摘要：" + summarize(userText) + "\n\n"
                + "模拟要点：\n"
                + "1. 用户注册登录、模型列表、对话历史、评分与报告页面可完整走通\n"
                + "2. 并排对比 / Prompt Lab / Battle 的 SSE 流式 UI 可正常演示\n"
                + "3. 配置 OPENROUTER_API_KEY 并将 AI_MOCK_ENABLED=false 后即可切换真实模型\n\n"
                + "```html\n"
                + "<!DOCTYPE html><html><body style=\"font-family:sans-serif;padding:24px\">"
                + "<h1>Mock Preview</h1><p>代码沙箱预览演示</p></body></html>\n"
                + "```\n"
                + (modelHint.isBlank() ? "" : "\n(options: " + modelHint + ")");
    }

    private String extractUserText(Prompt prompt) {
        if (prompt == null || prompt.getInstructions() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Message message : prompt.getInstructions()) {
            if (message.getText() != null) {
                sb.append(message.getText()).append('\n');
            }
        }
        return sb.toString();
    }

    private String summarize(String text) {
        if (text == null) {
            return "";
        }
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() <= 120 ? t : t.substring(0, 120) + "...";
    }

    private List<String> chunkText(String text, int size) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            chunks.add("");
            return chunks;
        }
        for (int i = 0; i < text.length(); i += size) {
            chunks.add(text.substring(i, Math.min(i + size, text.length())));
        }
        return chunks;
    }
}
