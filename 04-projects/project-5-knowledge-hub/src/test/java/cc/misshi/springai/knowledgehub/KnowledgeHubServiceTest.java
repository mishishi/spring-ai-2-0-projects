package cc.misshi.springai.knowledgehub;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeHubServiceTest {

    private final QueryRouter router = new QueryRouter();
    private final KnowledgeBase kb = new KnowledgeBase();
    private final CodeSnippetAnalyzer analyzer = new CodeSnippetAnalyzer();
    private final ConversationMemory memory = new ConversationMemory();
    private final KnowledgeHubService hub = new KnowledgeHubService(router, kb, analyzer, memory, null);

    @Test
    void docQaShouldReturnKnowledgeBaseHit() {
        var req = new KnowledgeHubService.HubRequest("s1", "RAG 的全称是什么?", null, null);
        var resp = hub.handle(req);
        assertThat(resp.route()).isEqualTo("DOC_QA");
        assertThat(resp.answer()).contains("Retrieval-Augmented Generation");
    }

    @Test
    void codeReviewShouldDetectSystemOut() {
        var req = new KnowledgeHubService.HubRequest(
                "s2", "请审查代码", "public void m() { System.out.println(\"hi\"); }", null);
        var resp = hub.handle(req);
        assertThat(resp.route()).isEqualTo("CODE_REVIEW");
        assertThat(resp.answer()).contains("System.out.println");
    }

    @Test
    void weeklyReportShouldFormatBulletPoints() {
        var req = new KnowledgeHubService.HubRequest(
                "s3", "生成周报", null, List.of("完成 A", "修复 B"));
        var resp = hub.handle(req);
        assertThat(resp.route()).isEqualTo("WEEKLY_REPORT");
        assertThat(resp.answer()).contains("完成 A", "修复 B");
    }

    @Test
    void chitchatShouldReturnGreeting() {
        var req = new KnowledgeHubService.HubRequest("s4", "你好", null, null);
        var resp = hub.handle(req);
        assertThat(resp.route()).isEqualTo("CHITCHAT");
        assertThat(resp.answer()).contains("你好");
    }

    @Test
    void memoryShouldKeepHistoryPerSession() {
        hub.handle(new KnowledgeHubService.HubRequest("alice", "什么是 RAG?", null, null));
        hub.handle(new KnowledgeHubService.HubRequest("alice", "怎么报销?", null, null));
        hub.handle(new KnowledgeHubService.HubRequest("bob", "什么是 MCP?", null, null));

        assertThat(memory.size("alice")).isEqualTo(4); // 2 user + 2 assistant
        assertThat(memory.size("bob")).isEqualTo(2);    // 1 user + 1 assistant
    }
}
