package cc.misshi.springai.chatclient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 纯本地测试,0 网络。
 *
 * <p>{@code @ActiveProfiles("test")} 让 CommandLineRunner 的 {@code @Profile("!test")} 生效,
 * 从而 3 个 LLM demo 不跑,Spring context 不会触发 ChatClient 真实调用。
 *
 * <p>{@code webEnvironment = NONE} 避免 Spring Boot Test 启动 mock servlet context
 * (chapter 2 还没引入 spring-boot-starter-web,推到 chapter 3)。
 *
 * <p>本章测试只验证 context 能起来;具体的 3 个 demo 行为在运行时看日志确认。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring context 能正常启动,且 ChatClient.Builder bean 已注入
    }
}
