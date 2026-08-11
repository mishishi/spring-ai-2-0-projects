package cc.misshi.springai.helloworld;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 纯本地测试,0 网络。
 *
 * <p>{@code @ActiveProfiles("test")} 让 CommandLineRunner 的 {@code @Profile("!test")} 生效,
 * 从而 CommandLineRunner 不注册,Spring context 不会触发 ChatClient 真实调用。
 *
 * <p>{@code webEnvironment = NONE} 避免 Spring Boot Test 启动 mock servlet context,
 * 这样 chapter 1 就不需要加 {@code spring-boot-starter-web} 依赖(后续 chapter 才需要)。
 *
 * <p>{@code spring.ai.openai.api-key=fake-key-for-tests} 是 application.yml 默认值,
 * 即使 LLM 被注入,也不会真去连 OpenAI(因为测试里 ChatClient 不会被调)。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring context 能正常启动
    }
}
