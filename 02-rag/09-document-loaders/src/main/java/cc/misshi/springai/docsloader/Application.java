package cc.misshi.springai.docsloader;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring AI 2.0 Document Loaders。
 *
 * <p>本章演示:
 * <ol>
 *   <li>用 {@link MarkdownDocumentReader} 读 .md</li>
 *   <li>用 {@link TikaDocumentReader} 读 .html(也支持 PDF / Word / PPT)</li>
 *   <li>用 {@link TokenTextSplitter} 按 token 切块(避免超 LLM 上下文)</li>
 *   <li>存到 pgvector + retrieve + QuestionAnswerAdvisor</li>
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
    CommandLineRunner docsLoaderDemo(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            @Value("classpath:/sample-docs/employee-handbook.md") Resource handbook,
            @Value("classpath:/sample-docs/tech-stack.md") Resource techStack,
            @Value("classpath:/sample-docs/faq.html") Resource faq) {
        return args -> {
            // ─── Demo 1: 读 Markdown + 按章节切块 ────────────────
            log.info("══════ Demo 1: MarkdownDocumentReader(按 ## 切分) ══════");
            MarkdownDocumentReaderConfig mdConfig = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(false)
                    .withIncludeBlockquote(false)
                    .build();
            MarkdownDocumentReader mdReader = new MarkdownDocumentReader(handbook, mdConfig);
            List<Document> mdDocs = mdReader.read();
            log.info("   读 employee-handbook.md → {} 个 Document(每 ## 一个)", mdDocs.size());
            mdDocs.forEach(d -> log.info("   📄 chunk 头: {}...", d.getText().substring(0, Math.min(50, d.getText().length()))));

            // ─── Demo 2: 读 HTML(Tika) ──────────────────────────
            log.info("══════ Demo 2: TikaDocumentReader(读 HTML) ══════");
            TikaDocumentReader tikaReader = new TikaDocumentReader(faq);
            List<Document> htmlDocs = tikaReader.read();
            log.info("   读 faq.html → {} 个 Document", htmlDocs.size());

            // ─── Demo 3: TokenTextSplitter 切块(每块 ~200 tokens) ─
            log.info("══════ Demo 3: TokenTextSplitter 切块 ══════");
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(200)
                    .withMinChunkSizeChars(50)
                    .withMinChunkLengthToEmbed(5)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();
            // 第 1 个 MD 文档太大,按 token 切
            List<Document> allRaw = new java.util.ArrayList<>(mdDocs);
            allRaw.addAll(htmlDocs);
            List<Document> chunks = splitter.apply(allRaw);
            log.info("   原始 {} 文档 → {} chunks(每块 ~200 tokens, overlap 50)", allRaw.size(), chunks.size());

            // ─── Step 4: 存到 pgvector ──────────────────────────
            log.info("══════ Step 4: 存到 pgvector ══════");
            vectorStore.add(chunks);
            log.info("   ✅ 全部 {} chunks 持久化", chunks.size());

            // ─── Demo 5: retrieve + QuestionAnswerAdvisor ────────
            log.info("══════ Demo 5: RAG 问答 ══════");
            ChatClient client = builder
                    .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                    .defaultSystem("你是 HR / IT 助手,基于公司文档回答")
                    .build();
            String[] questions = {
                    "试用期多久?",
                    "我笔记本坏了找谁?",
                    "年假可以拆开休吗?",
                    "技术栈用什么后端框架?"
            };
            for (String q : questions) {
                String r = client.prompt().user(q).call().content();
                log.info("Q: {}", q);
                log.info("🤖 {}", r);
                log.info("---");
            }

            // ─── Demo 6: 手动 retrieve 看 chunk 大小 ────────────
            log.info("══════ Demo 6: 手动 retrieve + 看 chunk 详情 ══════");
            List<Document> top = vectorStore.similaritySearch(
                    SearchRequest.builder().query("技术栈").topK(2).build());
            top.forEach(d -> {
                log.info("   📄 长度:{} chars,metadata:{}", d.getText().length(), d.getMetadata());
            });
        };
    }
}
