package cc.misshi.springai.promptadvisor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 纯本地测试,0 网络。
 *
 * <p>{@code @ActiveProfiles("test")} 让 CommandLineRunner 不跑,避免 LLM 真实调用。
 * <p>{@code webEnvironment = NONE} 避免 servlet context(chapter 3 还没加 web)。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // 验证 ChatClient.Builder + SimpleLoggerAdvisor bean 都能注入
    }
}
