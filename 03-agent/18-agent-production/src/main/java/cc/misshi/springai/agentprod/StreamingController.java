package cc.misshi.springai.agentprod;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 流式响应 + 记忆 + 错误处理 — 第 18 章.
 *
 * <p>3 大生产化能力:
 * <ol>
 *   <li>流式 SSE 响应(降低首字延迟)</li>
 *   <li>基于 conversationId 的多用户记忆隔离</li>
 *   <li>fallback 兜底(LLM 失败时返回安全回复)</li>
 * </ol>
 */
@RestController
@RequestMapping("/agent")
public class StreamingController {

    private final ChatClient chatClient;

    public StreamingController(ChatClient.Builder builder, ChatMemory memory) {
        this.chatClient = builder
                .defaultSystem("""
                        你是 helpful 助手。
                        """)
                .defaultAdvisors(
                    org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor.builder(memory).build()
                )
                .build();
    }

    /**
     * GET /agent/stream?conversationId=user-123&message=...
     * Server-Sent Events 流式响应
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam String conversationId,
            @RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    /**
     * GET /agent/ask?conversationId=user-123&message=...
     * 同步 + 错误兜底
     */
    @GetMapping("/ask")
    public Map<String, Object> ask(
            @RequestParam String conversationId,
            @RequestParam String message) {
        try {
            String response = chatClient.prompt()
                    .user(message)
                    .advisors(a -> a.param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            return Map.of("status", "ok", "response", response);
        } catch (Exception e) {
            // fallback:不暴露内部错误给用户
            return Map.of("status", "fallback", "response", "抱歉,服务暂时不可用,请稍后重试");
        }
    }
}
