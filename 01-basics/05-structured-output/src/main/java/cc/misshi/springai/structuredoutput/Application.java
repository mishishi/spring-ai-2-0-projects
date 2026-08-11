package cc.misshi.springai.structuredoutput;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Spring AI 2.0 Structured Output。
 *
 * <p>用 {@code .entity(Class)} / {@code .entity(ParameterizedTypeReference)} 把 LLM 输出
 * 转成强类型 Java 对象,不用自己解析 JSON。
 *
 * <p>本章 2 个 demo:
 * <ol>
 *   <li><b>单 POJO</b> — {@code .entity(Person.class)} 从文本提 Person</li>
 *   <li><b>POJO 列表</b> — {@code .entity(ParameterizedTypeReference<List<Movie>>)} 提 List</li>
 * </ol>
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
            ChatClient client = builder
                    .defaultSystem("你是一个信息提取助手,严格按照用户要求的格式输出")
                    .build();

            // ─── Demo 1: 单 POJO(entity(Class)) ──────────────────
            log.info("══════ Demo 1: entity(Person.class) ══════");
            Person person = client.prompt()
                    .user("""
                            从这句话提取人物信息:
                            "Bob 是一个 30 岁的 Java 工程师,业余喜欢爬山和摄影。"
                            """)
                    .call()
                    .entity(Person.class);
            log.info("🤖 {}", person);
            log.info("   name = {}", person.name());
            log.info("   age = {}", person.age());
            log.info("   occupation = {}", person.occupation());
            log.info("   hobby = {}", person.hobby());

            // ─── Demo 2: POJO 列表(entity(ParameterizedTypeReference)) ──
            log.info("══════ Demo 2: ParameterizedTypeReference<List<Movie>> ══════");
            List<Movie> movies = client.prompt()
                    .user("""
                            推荐 3 部经典科幻电影,按年份升序:
                            - 标题(英文)
                            - 年份
                            - 导演
                            - 评分(0-10)
                            """)
                    .call()
                    .entity(new org.springframework.core.ParameterizedTypeReference<List<Movie>>() {
                    });
            movies.forEach(m -> log.info("🤖 {} ({}) - {} - 评分 {}", m.title(), m.year(), m.director(), m.rating()));
        };
    }
}
