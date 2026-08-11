package cc.misshi.springai.structuredoutput;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 纯本地测试,0 网络。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // 验证 ChatClient.Builder + Person/Movie record bean 都能注入
    }
}
