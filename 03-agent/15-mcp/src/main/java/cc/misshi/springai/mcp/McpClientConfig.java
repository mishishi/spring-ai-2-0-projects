package cc.misshi.springai.mcp;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Client + ChatClient 装配 — 第 15 章.
 *
 * <p>spring-ai-starter-mcp-client 自动连接 application.yml 里配置的 MCP servers,
 * 通过 ToolCallbackProvider 暴露给 ChatClient.
 *
 * <p>注意:本模块自己既是 server 又是 client. 真实场景 server 跟 client
 * 通常是不同进程,client 通过 HTTP/stdio 连接.
 */
@Configuration
public class McpClientConfig {

    /**
     * ChatClient 注入从 MCP Client 自动发现的 tools.
     * 这样模型调用工具时,实际是通过 MCP 协议发到 MCP server,
     * 而不是直接调用本进程的 Java 方法.
     */
    @Bean
    public ChatClient mcpChatClient(ChatClient.Builder builder, ToolCallbackProvider mcpTools) {
        return builder
                .defaultSystem("""
                        你是一个办公助手,所有操作都通过 MCP 工具完成。
                        不要编造数据。
                        """)
                .defaultTools(mcpTools)
                .build();
    }
}
