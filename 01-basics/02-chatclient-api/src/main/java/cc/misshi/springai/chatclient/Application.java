package cc.misshi.springai.chatclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Spring AI 2.0 ChatClient API 深入。
 *
 * <p>本章用 3 个 demo 演示 ChatClient fluent API 的核心方法:
 * <ol>
 *   <li><b>基本 user prompt</b> — {@code prompt().user().call().content()}</li>
 *   <li><b>system + user 角色扮演</b> — {@code prompt().system().user()}</li>
 *   <li><b>流式响应</b> — {@code prompt().user().stream().content()}</li>
 * </ol>
 *
 * <p>运行时需要环境变量 {@code OPENAI_API_KEY}。
 */
@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 跑 3 个 ChatClient demo。测试 profile 不跑,避免连接真实 LLM。
     */
    @Bean
    @Profile("!test")
    CommandLineRunner chatClientDemos(ChatClient.Builder builder) {
        return args -> {
            ChatClient client = builder.build();

            // ─── Demo 1: 基本 user prompt ───────────────────────
            log.info("══════ Demo 1: 基本 user prompt ══════");
            String r1 = client.prompt()
                    .user("用一句话介绍 Spring AI 2.0")
                    .call()
                    .content();
            log.info("🤖 {}", r1);

            // ─── Demo 2: system + user 角色扮演 ─────────────────
            log.info("══════ Demo 2: system 角色扮演 ══════");
            String r2 = client.prompt()
                    .system("你是一个毒舌但靠谱的 Java 架构师,回答简短,不超过 50 字,带点讽刺")
                    .user("Spring Boot 启动慢怎么办?")
                    .call()
                    .content();
            log.info("🎭 {}", r2);

            // ─── Demo 3: 流式响应(streaming) ───────────────────
            log.info("══════ Demo 3: 流式响应(streaming) ══════");
            log.info("🤖 streaming:");
            client.prompt()
                    .user("用三句话介绍 Spring AI 的 3 个核心概念,每句不超过 20 字")
                    .stream()
                    .content()
                    .doOnNext(chunk -> log.info("   chunk: 「{}」", chunk))
                    .doOnComplete(() -> log.info("   (end of stream)"))
                    .blockLast();
        };
    }
}
