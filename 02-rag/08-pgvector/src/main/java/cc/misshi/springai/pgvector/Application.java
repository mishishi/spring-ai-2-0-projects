package cc.misshi.springai.pgvector;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI 2.0 pgvector。
 *
 * <p>把 ch7 的 SimpleVectorStore(内存)换成真实 PostgreSQL pgvector,
 * 数据持久化,可水平扩展,生产级方案。
 *
 * <p>本章 3 个 demo:
 * <ol>
 *   <li>把 5 个公司制度文档存到 pgvector</li>
 *   <li>真实 retrieve top-3</li>
 *   <li>QuestionAnswerAdvisor 全自动 RAG</li>
 * </ol>
 *
 * <p>前置条件:Docker pgvector 容器在 localhost:5433 跑(端口 5433 避开 macOS 本地 PG 5432)
 * {@code docker run -d --name pgvector -p 5433:5432 \
 *   -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=vectordb \
 *   pgvector/pgvector:pg16}
 */
@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 用 JdbcTemplate + EmbeddingModel 构造 PgVectorStore。
     *
     * <p>配置:
     * <ul>
     *   <li>维度 1536(OpenAI text-embedding-3-small)</li>
     *   <li>距离:COSINE(余弦,最常用)</li>
     *   <li>索引:HNSW(高性能,适合生产)</li>
     *   <li>initializeSchema:自动建 vector_store 表 + HNSW 索引</li>
     * </ul>
     */
    @Bean
    public VectorStore pgVectorStore(DataSource dataSource, EmbeddingModel embeddingModel) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .build();
    }

    @Bean
    @Profile("!test")
    CommandLineRunner ragDemos(ChatClient.Builder builder, VectorStore vectorStore) {
        return args -> {
            // 5 个公司制度文档(ch7 同款)
            List<Document> documents = List.of(
                    new Document("公司年假制度: 员工每年享有 10 天带薪年假,工作满 5 年增加至 15 天,满 10 年增加至 20 天,年假需提前 3 天申请"),
                    new Document("病假制度: 员工因病无法工作可申请病假,3 天以内无需医院证明,超过 3 天需提供医院诊断证明,病假期间工资按 80% 发放"),
                    new Document("加班制度: 工作日加班按 1.5 倍工资支付,周末加班按 2 倍工资支付,法定节假日加班按 3 倍工资支付,加班需提前申请,部门主管 + HR 双重审批"),
                    new Document("远程办公制度: 员工每周最多可远程办公 2 天,远程办公需提前 1 天申请,远程办公期间需保持在线,响应时间不超过 1 小时,重要会议仍需到办公室参加"),
                    new Document("报销制度: 出差需提前申请,经部门主管审批,交通费、住宿费、餐饮费均可报销,餐饮费每日上限 100 元,报销需在出差结束 7 天内提交")
            );

            // ─── Step 1: 存到 pgvector(真实持久化) ──────────────
            log.info("══════ Step 1: 存到 pgvector ══════");
            log.info("   准备存 {} 个文档", documents.size());
            vectorStore.add(documents);
            log.info("   ✅ 全部存到 pgvector(persistent on disk)");

            // ─── Demo 1: 真实 retrieve ─────────────────────────
            log.info("══════ Demo 1: pgvector retrieve top-3 ══════");
            String question = "我年假有几天?";
            List<Document> top3 = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(3)
                            .build());
            top3.forEach(d -> log.info("   📄 相似文档: {}...",
                    d.getText().substring(0, Math.min(40, d.getText().length()))));

            // ─── Demo 2: QuestionAnswerAdvisor 全自动 RAG ────────
            log.info("══════ Demo 2: QuestionAnswerAdvisor 全自动 RAG ══════");
            ChatClient client = builder
                    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                    .defaultSystem("你是 HR 助手,基于公司制度回答员工问题")
                    .build();
            String r = client.prompt().user(question).call().content();
            log.info("🤖 {}", r);

            // ─── Demo 3: 跨进程验证(pgvector 真持久化) ─────────
            log.info("══════ Demo 3: 重新跑 retrieve,数据应该还在 ══════");
            log.info("   (Spring AI 没缓存,每次都查 pgvector)");
            String[] questions = {
                    "出差餐饮费一天能报多少?",
                    "周末加班工资怎么算?",
                    "我能在家办公几天?"
            };
            for (String q : questions) {
                String ans = client.prompt().user(q).call().content();
                log.info("Q: {}", q);
                log.info("🤖 {}", ans);
                log.info("---");
            }

            log.info("══════ 提示 ══════");
            log.info("   数据已落 pgvector。重新跑 mvn spring-boot:run,数据还在(持久化!)");
            log.info("   清空: docker exec pgvector psql -U postgres -d vectordb -c 'TRUNCATE vector_store;'");
        };
    }
}
