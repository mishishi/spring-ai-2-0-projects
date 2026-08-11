package cc.misshi.springai.ragoverview;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 纯本地测试,0 网络。
 *
 * <p>注意:VectorStore bean 是延迟初始化的,只有第一次 add/query 时才连嵌入 API。
 * context 启动不会触发,所以 contextLoads OK。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // 验证 ChatClient.Builder + VectorStore(SimpleVectorStore)bean 都能注入
    }
}
