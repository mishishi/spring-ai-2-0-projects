package cc.misshi.springai.mcp;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * MCP Demo Controller — 第 15 章.
 *
 * <p>所有 /mcp/demo/* 端点都用 ChatClient 调 MCP 工具.
 */
@RestController
@RequestMapping("/mcp/demo")
public class McpDemoController {

    private final ChatClient mcpChatClient;

    public McpDemoController(ChatClient mcpChatClient) {
        this.mcpChatClient = mcpChatClient;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return mcpChatClient.prompt().user(question).call().content();
    }
}
