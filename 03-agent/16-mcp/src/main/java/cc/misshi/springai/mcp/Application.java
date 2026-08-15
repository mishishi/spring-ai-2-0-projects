package cc.misshi.springai.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第 15 章 · MCP 入口.
 *
 * <p>本模块同时跑 MCP Server + (可能再起) MCP Client:
 * <ul>
 *   <li><b>MCP Server</b>: Spring AI 自动把 @Tool 方法注册成 MCP 协议端点
 *     (默认 Streamable HTTP transport,替代旧 SSE)</li>
 *   <li><b>MCP Client</b>: 通过 spring-ai-starter-mcp-client 自动连接,
 *     注入到 ChatClient 的 defaultTools 里</li>
 * </ul>
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
