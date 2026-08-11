package cc.misshi.springai.ragprod;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring AI 2.0 RAG Production。
 *
 * <p>本章 3 个 demo:
 * <ol>
 *   <li>增量更新:用 document ID 去重,只 embed 新文档</li>
 *   <li>查询缓存:Spring Cache + Caffeine,重复 query 命中缓存</li>
 *   <li>监控:Spring Boot Actuator + Micrometer 暴露 RAG 指标</li>
 * </ol>
 */
@SpringBootApplication
@EnableCaching
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

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
    CommandLineRunner prodDemo(ChatClient.Builder builder, VectorStore vectorStore, RagService ragService) {
        return args -> {
            // ─── Demo 1: 增量更新 ────────────────────────────
            log.info("══════ Demo 1: 增量更新(用稳定 ID 去重) ══════");
            String docId1 = "policy-2026-v1";
            String docId2 = "policy-2026-v2";
            // 第 1 批:3 个 doc
            vectorStore.add(List.of(
                    new Document(docId1 + "-leave", "年假制度: 10 天带薪年假", java.util.Map.of("docId", docId1)),
                    new Document(docId1 + "-sick", "病假制度: 3 天不需证明", java.util.Map.of("docId", docId1)),
                    new Document(docId1 + "-expense", "报销制度: 餐饮日上限 100", java.util.Map.of("docId", docId1))
            ));
            log.info("   批量 1: 加 3 个 doc(policy-2026-v1)");

            // 第 2 批:更新 docId2(只 2 个)
            vectorStore.add(List.of(
                    new Document(docId2 + "-leave", "年假制度: 10 天带薪年假(2026 修订)", java.util.Map.of("docId", docId2)),
                    new Document(docId2 + "-sick", "病假制度: 3 天不需证明(2026 修订)", java.util.Map.of("docId", docId2))
            ));
            log.info("   批量 2: 加 2 个 doc(policy-2026-v2)");

            // 检查:pgvector 里有 5 个 doc(2 批没去重,因为 pgvector 默认 append)
            // 真实生产用 docId 维护一个"已 embed"清单,跳过已 embed 的
            log.info("   (生产实践:用 docId 清单维护已 embed,跳过)");

            // ─── Demo 2: 查询缓存 ────────────────────────────
            log.info("══════ Demo 2: 查询缓存(Spring Cache) ══════");
            long t1 = System.currentTimeMillis();
            String r1 = ragService.ragWithCache("年假怎么算?", vectorStore, builder);
            long t2 = System.currentTimeMillis();
            log.info("   首次查询: {}ms", t2 - t1);

            long t3 = System.currentTimeMillis();
            String r2 = ragService.ragWithCache("年假怎么算?", vectorStore, builder);  // 命中缓存
            long t4 = System.currentTimeMillis();
            log.info("   二次查询(应命中缓存): {}ms", t4 - t3);
            log.info("   缓存命中加速约 {}x", (t2 - t1) / Math.max(1, (t4 - t3)));

            // ─── Demo 3: 监控 ────────────────────────────────
            log.info("══════ Demo 3: 监控 ══════");
            log.info("   启用 Spring Boot Actuator: GET /actuator/health /metrics /prometheus");
            log.info("   Spring AI 2.0 内置 Micrometer 指标:ai.chat.client.call / ai.vector.store.query / etc");
            log.info("   (浏览器访问 http://localhost:8080/actuator/metrics 看)");
        };
    }
}

@Service
class RagService {
    @Cacheable("ragQueries")
    public String ragWithCache(String question, VectorStore vectorStore, ChatClient.Builder builder) {
        // 模拟慢查询
        ChatClient client = builder
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
        return client.prompt().user(question).call().content();
    }
}

@RestController
@RequestMapping("/api/rag")
class RagController {
    private final ChatClient client;
    private final VectorStore vectorStore;

    RagController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.client = builder
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String q) {
        return client.prompt().user(q).call().content();
    }
}
