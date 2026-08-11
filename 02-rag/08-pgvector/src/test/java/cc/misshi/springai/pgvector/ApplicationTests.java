package cc.misshi.springai.pgvector;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 集成测试:连真实 pgvector(localhost:5432)。
 *
 * <p>前置条件:Docker pgvector container 在跑(端口 5433,避开本地 PG 5432)
 * {@code docker run -d --name pgvector -p 5433:5432 \
 *   -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=vectordb \
 *   pgvector/pgvector:pg16}
 *
 * <p>不调 OpenAI(VectorStore 延迟初始化),0 网络。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring context + DataSource + PgVectorStore + EmbeddingModel bean
    }
}
