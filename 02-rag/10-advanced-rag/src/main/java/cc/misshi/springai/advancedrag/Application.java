package cc.misshi.springai.advancedrag;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
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
 * Spring AI 2.0 Advanced RAG。
 *
 * <p>Spring AI 2.0 引入 {@code RetrievalAugmentationAdvisor},可组合:
 * <ul>
 *   <li>{@code QueryExpander}(如 MultiQueryExpander)— 把 1 个 query 扩展成 N 个</li>
 *   <li>{@code DocumentRetriever}(如 VectorStoreDocumentRetriever)— 从 vector store 取</li>
 *   <li>{@code DocumentJoiner}(如 ConcatenationDocumentJoiner)— 合并多路结果</li>
 *   <li>{@code QueryAugmenter}(如 ContextualQueryAugmenter)— 拼进 prompt</li>
 * </ul>
 *
 * <p>本章 3 个 demo 对比:
 * <ol>
 *   <li>基础 RAG(QuestionAnswerAdvisor,跟 ch8/9 一样)</li>
 *   <li>Multi-Query RAG(1 query → 3 query → 3 检索 → 合并)</li>
 *   <li>RetrievalAugmentationAdvisor 完整组合</li>
 * </ol>
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
    CommandLineRunner advancedRagDemo(ChatClient.Builder builder, VectorStore vectorStore) {
        return args -> {
            // ─── 准备数据(跟 ch8/9 类似,但 5 个文档) ──────────
            log.info("══════ Step 1: 准备 5 文档 + 存 pgvector ══════");
            List<Document> documents = List.of(
                    new Document("公司年假制度: 员工每年享有 10 天带薪年假,工作满 5 年增加至 15 天,满 10 年增加至 20 天"),
                    new Document("病假制度: 3 天以内无需医院证明,超过 3 天需医院证明,病假期间工资按 80% 发放"),
                    new Document("加班制度: 工作日 1.5 倍,周末 2 倍,法定节假日 3 倍,需提前申请部门主管 + HR 双重审批"),
                    new Document("远程办公制度: 每周最多 2 天,需提前 1 天申请,在线响应时间不超过 1 小时,重要会议需到办公室"),
                    new Document("报销制度: 出差需提前申请,交通住宿餐饮可报,餐饮日上限 100 元,7 天内提交")
            );
            // 简单:不 truncate,直接 add(数据小,重复无所谓)
            vectorStore.add(documents);
            log.info("   5 文档已存");

            // ─── Demo 1: 基础 RAG(对比基线) ─────────────────
            log.info("══════ Demo 1: 基础 RAG(基线对比) ══════");
            ChatClient baseClient = builder
                    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                    .defaultSystem("你是 HR 助手")
                    .build();
            String vague = "我工作满 6 年,能休几天假?";
            log.info("Q: {}", vague);
            String r1 = baseClient.prompt().user(vague).call().content();
            log.info("🤖 base: {}", r1);

            // ─── Demo 2: Multi-Query RAG ─────────────────────
            log.info("══════ Demo 2: Multi-Query RAG(1 query 扩 3 个) ══════");
            // 先用 expander 扩成 3 个 query
            MultiQueryExpander expander = MultiQueryExpander.builder()
                    .chatClientBuilder(builder)
                    .numberOfQueries(3)
                    .build();
            List<Query> expandedQueries = expander.expand(new Query(vague));
            log.info("   原始: {}", vague);
            log.info("   扩展: {}", expandedQueries);

            // 3 个 query 各自 retrieve,合并
            List<Document> allDocs = new java.util.ArrayList<>();
            for (Query q : expandedQueries) {
                List<Document> docs = vectorStore.similaritySearch(
                        SearchRequest.builder().query(q.text()).topK(2).build());
                allDocs.addAll(docs);
            }
            // 去重
            List<Document> deduped = allDocs.stream().distinct().toList();
            log.info("   3 query 合并后(去重): {} docs", deduped.size());
            String context = deduped.stream()
                    .map(d -> "- " + d.getText())
                    .reduce("", (a, b) -> a + "\n" + b);
            String r2 = builder.build().prompt()
                    .system("基于以下 context 回答:\n" + context)
                    .user(vague)
                    .call().content();
            log.info("🤖 multi-query: {}", r2);

            // ─── Demo 3: RetrievalAugmentationAdvisor(2.0 组合式) ──
            log.info("══════ Demo 3: RetrievalAugmentationAdvisor(2.0 组合式) ══════");
            RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                    .queryExpander(MultiQueryExpander.builder()
                            .chatClientBuilder(builder)
                            .numberOfQueries(3)
                            .build())
                    .documentRetriever(VectorStoreDocumentRetriever.builder()
                            .vectorStore(vectorStore)
                            .topK(3)
                            .similarityThreshold(0.5)
                            .build())
                    .documentJoiner(new ConcatenationDocumentJoiner())
                    .queryAugmenter(ContextualQueryAugmenter.builder().build())
                    .build();
            ChatClient advancedClient = builder
                    .defaultAdvisors(advisor)
                    .build();
            log.info("Q: {}", vague);
            String r3 = advancedClient.prompt().user(vague).call().content();
            log.info("🤖 advanced: {}", r3);

            log.info("══════ 总结 ══════");
            log.info("   基础 RAG: 1 query → 1 检索");
            log.info("   Multi-Query: 1 query → 3 query → 3 检索 → 合并(更全)");
            log.info("   RetrievalAugmentationAdvisor: 2.0 一行配置组合(更灵活)");
        };
    }
}
