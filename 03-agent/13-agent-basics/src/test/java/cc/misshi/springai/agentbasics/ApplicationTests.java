package cc.misshi.springai.agentbasics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring context 启动测试 — 验证所有 @Bean 装配正确.
 * 0 网络,默认 fake API key 不会触发真实 LLM 调用.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=fake-key-for-context-test"
})
class ApplicationTests {

    @Test
    void contextLoads() {
        // Spring 上下文能成功启动,所有 @Bean 装配 OK
    }
}
