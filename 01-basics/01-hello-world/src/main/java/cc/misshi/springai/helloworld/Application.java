package cc.misshi.springai.helloworld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Spring AI 2.0 Hello World。
 *
 * <p>本 chapter 目标:
 * <ul>
 *   <li>跑起来,看到 LLM 输出</li>
 *   <li>理解 ChatClient 的基本用法(builder / prompt / call / content)</li>
 *   <li>知道怎么换 LLM(改 pom 依赖 + application.yml)</li>
 * </ul>
 *
 * <p>运行时需要环境变量 {@code OPENAI_API_KEY}(或修改 application.yml 用别的 LLM)。
 */
@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Hello World demo:用一句话让 LLM 介绍 Spring AI 2.0。
     *
     * <p>默认 profile 启动时跑;测试 profile 不跑,避免连接真实 LLM。
     */
    @Bean
    @Profile("!test")
    CommandLineRunner helloWorld(ChatClient.Builder builder) {
        return args -> {
            ChatClient client = builder.build();
            String response = client.prompt()
                    .user("用一句话介绍 Spring AI 2.0,不超过 30 字")
                    .call()
                    .content();
            log.info("🤖 Spring AI says: {}", response);
        };
    }
}
