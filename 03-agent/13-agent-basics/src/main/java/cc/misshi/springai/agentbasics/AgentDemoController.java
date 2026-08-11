package cc.misshi.springai.agentbasics;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent Demo Controller — 第 13 章.
 *
 * <p>展示 Agent loop 的核心:用户问问题 → ChatClient 自动判断要不要调工具
 * → 调哪个工具 → 拿到结果 → 整理成自然语言回答.
 *
 * <p>完全 0 网络本地测试请用 {@link AgentDemoServiceTest}.
 */
@RestController
@RequestMapping("/agent")
public class AgentDemoController {

    private final ChatClient chatClient;

    public AgentDemoController(ChatClient.Builder builder, ToolCallbackProvider tools) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一个 helpful 的助手。
                        回答时优先使用可用工具,不要编造数据。
                        """)
                .defaultTools(tools)
                .build();
    }

    /**
     * GET /agent/ask?question=...
     */
    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
