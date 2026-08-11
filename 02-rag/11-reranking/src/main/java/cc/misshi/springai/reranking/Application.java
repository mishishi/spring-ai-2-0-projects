package cc.misshi.springai.reranking;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
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
 * Spring AI 2.0 Re-ranking。
 *
 * <p>用 {@link org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor}
 * 在 RAG 检索后、prompt 拼装前重排序,提升 top-k 准确率。
 *
 * <p>本章用 {@link KeywordRerankProcessor}(关键词命中加权)做 demo,
 * 真实生产用 Cohere Rerank / BGE Reranker(via TransformersEmbeddingModel)。
 */
@SpringBootApplication
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
    CommandLineRunner rerankDemo(ChatClient.Builder builder,
                                 VectorStore vectorStore,
                                 KeywordRerankProcessor rerankProcessor) {
        return args -> {
            // ─── 准备数据 ──────────────────────────────────────
            log.info("══════ Step 1: 准备 6 文档 + 存 pgvector ══════");
            List<Document> documents = List.of(
                    new Document("年假制度: 员工每年 10 天带薪年假,满 5 年 15 天,满 10 年 20 天"),
                    new Document("病假制度: 3 天内不需证明,超 3 天需医院证明,工资按 80% 发放"),
                    new Document("加班制度: 工作日 1.5 倍,周末 2 倍,节假日 3 倍,需提前申请"),
                    new Document("远程办公: 每周最多 2 天,提前 1 天申请,响应时间不超 1 小时"),
                    new Document("报销制度: 出差提前申请,交通住宿餐饮可报,餐饮日上限 100 元"),
                    new Document("春节假期: 法定 7 天,公司额外给 3 天福利假,共 10 天带薪")
            );
            vectorStore.add(documents);
            log.info("   6 文档已存");

            // ─── Demo 1: 不用 Re-rank(基线) ───────────────────
            log.info("══════ Demo 1: 不用 Re-rank(基线) ══════");
            String question = "我工作满 6 年,年假怎么算?春节能休几天?";
            List<Document> baseline = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(4).build());
            log.info("   top-4(按 embedding 相似度):");
            for (int i = 0; i < baseline.size(); i++) {
                log.info("   [{}] {}", i + 1, baseline.get(i).getText().substring(0, Math.min(50, baseline.get(i).getText().length())));
            }

            // ─── Demo 2: 用 KeywordRerankProcessor 重排序 ──────
            log.info("══════ Demo 2: 用 KeywordRerankProcessor 重排序 ══════");
            List<Document> reranked = rerankProcessor.process(
                    new org.springframework.ai.rag.Query(question), baseline);
            log.info("   重排后 top-4(按关键词命中加权):");
            for (int i = 0; i < reranked.size(); i++) {
                log.info("   [{}] {}", i + 1, reranked.get(i).getText().substring(0, Math.min(50, reranked.get(i).getText().length())));
            }

            // ─── Demo 3: RetrievalAugmentationAdvisor 集成 re-rank ─
            log.info("══════ Demo 3: RAG 集成 re-rank(2.0 advisor) ══════");
            RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                    .documentRetriever(VectorStoreDocumentRetriever.builder()
                            .vectorStore(vectorStore)
                            .topK(5)
                            .build())
                    .documentPostProcessors(rerankProcessor)
                    .build();
            ChatClient client = builder.defaultAdvisors(advisor).build();
            String r = client.prompt().user(question).call().content();
            log.info("🤖 {}", r);

            log.info("══════ 总结 ══════");
            log.info("   Re-rank 提升 top-k 准确率:把真正相关的文档排到前面");
            log.info("   生产推荐: Cohere Rerank API (云) 或 BGE Reranker (本地 ONNX)");
        };
    }
}
