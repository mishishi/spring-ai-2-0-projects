package cc.misshi.springai.functioncalling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Spring AI 2.0 Function Calling。
 *
 * <p>用 {@code @Tool} 注解把 Java 方法暴露给 LLM,LLM 自动决定何时调用、调哪个。
 *
 * <p>本章 3 个 demo:
 * <ol>
 *   <li><b>时间工具</b> — 用户问"现在几点了",LLM 自动调 getCurrentTime()</li>
 *   <li><b>数学计算</b> — 用户问"23 + 45",LLM 自动调 add(23, 45)</li>
 *   <li><b>工具组合</b> — 复杂问题 LLM 调多个工具 + 综合回答</li>
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

    @Bean
    @Profile("!test")
    CommandLineRunner demos(ChatClient.Builder builder) {
        return args -> {
            // 全局配工具:builder.defaultTools() 让 LLM 自动发现
            ChatClient client = builder
                    .defaultTools(new TimeTools(), new MathTools())
                    .defaultSystem("你是一个友好的助手,回答简短")
                    .build();

            // ─── Demo 1: 时间工具(@Tool 注解) ─────────────────
            log.info("══════ Demo 1: 时间工具 ══════");
            String r1 = client.prompt()
                    .user("现在几点了?今天是几号?")
                    .call()
                    .content();
            log.info("🤖 {}", r1);

            // ─── Demo 2: 数学计算(@Tool + @ToolParam) ─────────
            log.info("══════ Demo 2: 数学计算 ══════");
            String r2 = client.prompt()
                    .user("请帮我算 23 + 45 等于多少?")
                    .call()
                    .content();
            log.info("🤖 {}", r2);

            // ─── Demo 3: 工具组合(LLM 调多个) ─────────────────
            log.info("══════ Demo 3: 工具组合 ══════");
            String r3 = client.prompt()
                    .user("现在是 2026 年,我 2000 年出生,请算我的年龄(到当前年份)")
                    .call()
                    .content();
            log.info("🤖 {}", r3);
        };
    }
}
