package cc.misshi.springai.streaming;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

/**
 * Chat 流式 / 同步 端点。
 *
 * <p>核心对比:
 * <ul>
 *   <li>{@code /sync} — {@code .call().content()} 返回 String,LLM 完整生成后一次性返回</li>
 *   <li>{@code /stream} — {@code .stream().content()} 返回 Flux&lt;String&gt;,边生成边推</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient client;

    public ChatController(ChatClient.Builder builder) {
        this.client = builder.build();
    }

    /**
     * 同步端点:等 LLM 完整生成,一次性返回。
     */
    @GetMapping("/sync")
    public String sync(@RequestParam(defaultValue = "用一句话介绍 Spring AI") String q) {
        return client.prompt()
                .user(q)
                .call()
                .content();
    }

    /**
     * 流式端点:边生成边推,SSE 协议,前端用 EventSource 接收。
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam(defaultValue = "用三句话介绍 Spring AI") String q) {
        return client.prompt()
                .user(q)
                .stream()
                .content();
    }
}
