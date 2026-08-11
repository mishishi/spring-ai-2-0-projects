package cc.misshi.springai.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 纯本地测试,0 网络。
 *
 * <p>Chapter 6 加了 webflux,但 test profile 不调 LLM,context 启动 OK。
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // 验证 WebFlux + ChatClient + Controllers 都加载
    }
}
