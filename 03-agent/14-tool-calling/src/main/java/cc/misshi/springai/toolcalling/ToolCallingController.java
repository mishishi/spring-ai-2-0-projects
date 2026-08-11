package cc.misshi.springai.toolcalling;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Tool Calling Demo Controller — 第 14 章.
 *
 * <p>展示 5 大特性的 REST 入口.
 */
@RestController
@RequestMapping("/tool")
public class ToolCallingController {

    private final ChatClient chatClient;

    public ToolCallingController(ChatClient.Builder builder, ToolCallbackProvider tools) {
        this.chatClient = builder
                .defaultSystem("""
                        你是企业助手,严格使用工具回答,不要编造数据。
                        如果工具返回错误,直接告诉用户。
                        """)
                .defaultTools(tools)
                .build();
    }

    /**
     * GET /tool/ask?question=...
     */
    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return chatClient.prompt().user(question).call().content();
    }

    /**
     * GET /tool/ask-with-context?question=...&tenantId=acme
     * 演示 ToolContext 注入(把 tenantId 传给 listMyCustomers).
     */
    @GetMapping("/ask-with-context")
    public String askWithContext(
            @RequestParam String question,
            @RequestParam String tenantId) {
        return chatClient.prompt()
                .user(question)
                .toolContext(Map.of("tenantId", tenantId))
                .call()
                .content();
    }
}
