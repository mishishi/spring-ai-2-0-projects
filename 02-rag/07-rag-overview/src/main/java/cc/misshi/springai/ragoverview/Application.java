package cc.misshi.springai.ragoverview;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Spring AI 2.0 RAG Overview。
 *
 * <p>RAG(Retrieval-Augmented Generation)4 步:
 * <ol>
 *   <li><b>Load</b> — DocumentReader 加载文档(PDF/Word/Markdown...)</li>
 *   <li><b>Split</b> — DocumentSplitter 切块(避免超 LLM context)</li>
 *   <li><b>Embed</b> — EmbeddingModel 把每个 chunk 转成向量</li>
 *   <li><b>Store</b> — VectorStore 存向量 + 元数据</li>
 * </ol>
 * 然后用户提问时:<b>Retrieve</b> 相似 chunks → 拼进 prompt → LLM 答。
 *
 * <p>本章用 {@code SimpleVectorStore}(内存,无需 Docker / DB)演示完整 RAG 流程。
 * 真实生产用 pgvector / Milvus / Chroma(后续 chapter 8+ 讲)。
 */
@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * RAG 4 步 + 3 个 demo。
     */
    @Bean
    @Profile("!test")
    CommandLineRunner ragDemos(ChatClient.Builder builder, VectorStore vectorStore) {
        return args -> {
            // ─── Step 1: Load(准备 5 个公司制度文档) ──────────────
            log.info("══════ Step 1: Load 文档 ══════");
            List<Document> documents = List.of(
                    new Document("""
                            公司年假制度:
                            - 员工每年享有 10 天带薪年假
                            - 工作满 5 年,年假增加至 15 天
                            - 工作满 10 年,年假增加至 20 天
                            - 年假需提前 3 天申请,部门主管审批
                            """),
                    new Document("""
                            病假制度:
                            - 员工因病无法工作可申请病假
                            - 3 天以内病假无需医院证明
                            - 超过 3 天需提供医院诊断证明
                            - 病假期间工资按 80% 发放
                            """),
                    new Document("""
                            加班制度:
                            - 工作日加班按 1.5 倍工资支付
                            - 周末加班按 2 倍工资支付
                            - 法定节假日加班按 3 倍工资支付
                            - 加班需提前申请,部门主管 + HR 双重审批
                            """),
                    new Document("""
                            远程办公制度:
                            - 员工每周最多可远程办公 2 天
                            - 远程办公需提前 1 天申请
                            - 远程办公期间需保持在线,响应时间不超过 1 小时
                            - 重要会议仍需到办公室参加
                            """),
                    new Document("""
                            报销制度:
                            - 出差需提前申请,经部门主管审批
                            - 交通费、住宿费、餐饮费均可报销
                            - 餐饮费每日上限 100 元
                            - 报销需在出差结束 7 天内提交
                            """)
            );
            log.info("   加载 {} 个文档", documents.size());

            // ─── Step 2: Split(简单按文档切,生产用 TokenTextSplitter) ──
            log.info("══════ Step 2: Split 切块(本 demo 不切,每 doc 当一个 chunk) ══════");

            // ─── Step 3: Embed + Step 4: Store(自动,SimpleVectorStore 触发) ──
            log.info("══════ Step 3+4: Embed + Store(自动) ══════");
            vectorStore.add(documents);
            log.info("   全部存到 SimpleVectorStore(in-memory)");

            // ─── Demo 1: 手动 retrieve(SearchRequest) ───────────
            log.info("══════ Demo 1: 手动 retrieve top-3 ══════");
            String question1 = "我年假有几天?";
            List<Document> top3 = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question1)
                            .topK(3)
                            .build());
            top3.forEach(d -> log.info("   📄 相似文档: {}...", d.getText().substring(0, Math.min(40, d.getText().length()))));
            log.info("   (LLM 会用这 3 个文档回答)");

            // ─── Demo 2: QuestionAnswerAdvisor(全自动 RAG) ───────
            log.info("══════ Demo 2: QuestionAnswerAdvisor 自动 RAG ══════");
            ChatClient client = builder
                    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                    .defaultSystem("你是 HR 助手,基于公司制度回答员工问题")
                    .build();
            String r2 = client.prompt()
                    .user(question1)
                    .call()
                    .content();
            log.info("🤖 {}", r2);

            // ─── Demo 3: 多问题,看 RAG 检索效果 ─────────────────
            log.info("══════ Demo 3: 多问题验证 RAG ══════");
            String[] questions = {
                    "出差餐饮费一天能报多少?",
                    "周末加班工资怎么算?",
                    "我能在家办公几天?"
            };
            for (String q : questions) {
                String r = client.prompt().user(q).call().content();
                log.info("Q: {}", q);
                log.info("🤖 {}", r);
                log.info("---");
            }
        };
    }
}
