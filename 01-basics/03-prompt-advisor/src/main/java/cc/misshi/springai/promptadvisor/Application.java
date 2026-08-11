package cc.misshi.springai.promptadvisor;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Spring AI 2.0 PromptTemplate + Advisor 模式。
 *
 * <p>本章用 2 个 demo 演示两个核心概念:
 * <ol>
 *   <li><b>PromptTemplate</b> — 参数化 prompt(类似 Thymeleaf / Freemarker)</li>
 *   <li><b>SimpleLoggerAdvisor</b> — Advisor 拦截器模式(类比 Spring AOP),
 *       在 LLM 调用前后自动记录日志</li>
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
     * 跑 2 个 demo:test profile 不跑,避免真实 LLM 调用。
     */
    @Bean
    @Profile("!test")
    CommandLineRunner demos(ChatClient.Builder builder) {
        return args -> {
            // 全局配一个 SimpleLoggerAdvisor:每次 LLM 调用前后自动打日志
            ChatClient client = builder
                    .defaultAdvisors(new SimpleLoggerAdvisor())
                    .defaultSystem("你是一个友好的助手,回答简短,不超过 50 字")
                    .build();

            // ─── Demo 1: PromptTemplate 参数化 ──────────────────
            log.info("══════ Demo 1: PromptTemplate 参数化 ══════");
            String templateString = """
                    你是一个 {role}。
                    用户名字: {name}
                    用户问题: {question}
                    请用 {style} 风格回答。
                    """;
            PromptTemplate template = new PromptTemplate(templateString);
            String userInput = template.render(Map.of(
                    "role", "Java 架构师",
                    "name", "Alice",
                    "question", "Spring Boot 启动慢怎么办?",
                    "style", "通俗易懂,带点幽默"));
            log.info("模板渲染后:\n{}", userInput);
            String r1 = client.prompt()
                    .user(userInput)
                    .call()
                    .content();
            log.info("🤖 {}", r1);

            // ─── Demo 2: Advisor 拦截器模式 ──────────────────────
            log.info("══════ Demo 2: SimpleLoggerAdvisor 自动打日志 ══════");
            log.info("(看控制台 SimpleLoggerAdvisor 的 before/after 日志)");
            String r2 = client.prompt()
                    .user("用一句话介绍 Spring AI 的 Advisor 模式")
                    .call()
                    .content();
            log.info("🤖 {}", r2);
        };
    }
}
