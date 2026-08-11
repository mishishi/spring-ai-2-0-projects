package cc.misshi.springai.streaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI 2.0 Streaming。
 *
 * <p>本章用 Spring WebFlux + SSE(Server-Sent Events)把 LLM 输出实时推给浏览器,
 * 实现"打字机效果"。
 *
 * <p>3 个 demo 端点:
 * <ul>
 *   <li>{@code GET /api/chat/sync?q=...} — 同步(对比)</li>
 *   <li>{@code GET /api/chat/stream?q=...} — 流式(SSE)</li>
 *   <li>{@code GET /} — 简单 HTML 页面,接 stream 端点演示打字机</li>
 * </ul>
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
