package cc.misshi.springai.knowledgehub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Knowledge Hub Service — 路由 query 到不同 handler.
 *
 * <p>架构:QueryRouter → 选路由 → 调对应 handler(DocQA / CodeReview / WeeklyReport / Chitchat).
 * <p>每个 handler 都用 0 网络 mock 实现,真实 LLM 时切到 ChatClient.
 */
@Service
public class KnowledgeHubService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeHubService.class);

    private final QueryRouter router;
    private final KnowledgeBase knowledgeBase;
    private final CodeSnippetAnalyzer codeAnalyzer;
    private final ConversationMemory memory;
    private final ChatClient chatClient;

    public KnowledgeHubService(QueryRouter router,
                               KnowledgeBase knowledgeBase,
                               CodeSnippetAnalyzer codeAnalyzer,
                               ConversationMemory memory,
                               @Autowired(required = false) ChatClient.Builder builder) {
        this.router = router;
        this.knowledgeBase = knowledgeBase;
        this.codeAnalyzer = codeAnalyzer;
        this.memory = memory;
        this.chatClient = (builder == null) ? null : builder
                .defaultSystem("""
                        你是 KnowledgeHub 综合 AI 助手。
                        根据用户问题类型(文档问答/代码审查/周报生成/闲聊),针对性回答。
                        """)
                .build();
    }

    /**
     * 处理请求:路由 → handler → 记忆 → 返回.
     */
    public HubResponse handle(HubRequest request) {
        String sessionId = request.sessionId() == null ? "default" : request.sessionId();
        String query = request.query() == null ? "" : request.query();

        // 1. 记忆
        memory.add(sessionId, "user", query);

        // 2. 路由
        QueryRouter.Route route = router.route(query);
        log.info("[Hub] session={} query={} → route={}", sessionId, query, route);

        // 3. 处理
        String answer = switch (route) {
            case DOC_QA -> handleDocQa(query);
            case CODE_REVIEW -> handleCodeReview(query, request.codeSnippet());
            case WEEKLY_REPORT -> handleWeeklyReport(query, request.bulletPoints());
            case CHITCHAT -> handleChitchat(query);
            case UNKNOWN -> "我没有理解你的问题,试试问「什么是 X」或粘贴一段代码让我审查。";
        };

        // 4. 记忆
        memory.add(sessionId, "assistant", answer);

        // 5. 上下文
        List<ConversationMemory.Entry> history = memory.recent(sessionId);
        return new HubResponse(route.name(), answer, history, knowledgeBase.size());
    }

    // ─── Handlers ──────────────────────────────────────

    private String handleDocQa(String query) {
        List<KnowledgeBase.Doc> hits = knowledgeBase.search(query, 3);
        if (hits.isEmpty()) {
            return "知识库里没找到相关内容。试试问:Spring AI 是什么? 怎么报销? 加班工资怎么算?";
        }
        StringBuilder sb = new StringBuilder("【知识库回答】\n");
        for (KnowledgeBase.Doc d : hits) {
            sb.append("> ").append(d.text()).append("\n\n");
        }
        sb.append("【总结】").append(hits.get(0).text());
        return sb.toString();
    }

    private String handleCodeReview(String query, String code) {
        if (code == null || code.isBlank()) {
            return "【代码审查】请提供代码片段(放在 codeSnippet 字段)。示例:`public void m() { System.out.println(\"hi\"); }`";
        }
        var summary = codeAnalyzer.review(code);
        StringBuilder sb = new StringBuilder("【代码审查报告】\n\n");
        sb.append("- 总行数: ").append(summary.lines()).append("\n");
        sb.append("- 圈复杂度: ").append(summary.complexity()).append("\n");
        if (summary.issues().isEmpty()) {
            sb.append("- 问题: 无明显问题 ✅\n");
        } else {
            sb.append("- 问题: ").append(summary.issues().size()).append(" 个\n");
            for (String issue : summary.issues()) {
                sb.append("  - ⚠️ ").append(issue).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String handleWeeklyReport(String query, List<String> bulletPoints) {
        if (bulletPoints == null || bulletPoints.isEmpty()) {
            return "【周报生成】请在 bulletPoints 字段提供要点列表。";
        }
        StringBuilder sb = new StringBuilder("【本周工作周报】\n\n");
        sb.append("## 完成事项\n");
        for (String p : bulletPoints) {
            sb.append("- ").append(p).append("\n");
        }
        sb.append("\n## 下周计划\n");
        sb.append("- 继续推进上述工作,补充遗留项\n");
        sb.append("- 团队协作与 code review\n");
        return sb.toString();
    }

    private String handleChitchat(String query) {
        return "你好!我是 KnowledgeHub,可以帮你:\n- 文档问答(问「什么是 RAG」)\n- 代码审查(粘贴代码)\n- 周报生成(给要点列表)\n试试问点什么吧 👋";
    }

    // ─── DTO ───────────────────────────────────────────

    public record HubRequest(String sessionId, String query, String codeSnippet, List<String> bulletPoints) {}

    public record HubResponse(
            String route,
            String answer,
            List<ConversationMemory.Entry> history,
            int knowledgeBaseSize
    ) {}
}
