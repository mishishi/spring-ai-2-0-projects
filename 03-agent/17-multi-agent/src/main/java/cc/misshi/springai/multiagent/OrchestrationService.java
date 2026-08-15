package cc.misshi.springai.multiagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 编程式 Orchestrator — 第 16 章.
 *
 * <p>真实 LLM 调 sub-agent 走工具,但这里用 0 网络可测的编程式版本:
 * orchestrator 不直接调 LLM,而是按固定流程串接 3 个 sub-agent.
 * 0 网络测试只验证流程编排逻辑,实际 sub-agent 内容需要真实 LLM.
 */
@Service
public class OrchestrationService {

    private final ChatClient researcher;
    private final ChatClient writer;
    private final ChatClient reviewer;
    private final ChatClient orchestrator;

    public OrchestrationService(
            @Qualifier("researcherAgent") ChatClient researcher,
            @Qualifier("writerAgent") ChatClient writer,
            @Qualifier("reviewerAgent") ChatClient reviewer,
            @Qualifier("orchestratorAgent") ChatClient orchestrator) {
        this.researcher = researcher;
        this.writer = writer;
        this.reviewer = reviewer;
        this.orchestrator = orchestrator;
    }

    /**
     * Pipeline: research → write → review → final answer.
     * 0 网络测试: 用固定的 mock 内容走完流程,验证 3 个 sub-agent + 1 orchestrator 都被调用.
     */
    public PipelineResult runPipeline(String userRequest, String mockedResearch) {
        // 1. Research — 用传入的 mockedResearch(0 网络可测)
        // 真实 LLM: researcher.prompt(userRequest).call().content();
        String facts = mockedResearch;

        // 2. Write
        // 真实 LLM: writer.prompt("事实清单:\\n" + facts).call().content();
        String draft = "[MOCK DRAFT based on " + facts.length() + " chars] " + facts.substring(0, Math.min(50, facts.length())) + "...";

        // 3. Review
        // 真实 LLM: reviewer.prompt("审稿:\\n" + draft).call().content();
        String review = "[MOCK REVIEW] 事实清晰, 建议增加第 2 段细节";

        // 4. Orchestrator 整合(也是 mocked,因为 sub-agent 都是 mocked)
        String finalAnswer = "【研究报告】\n\n" + draft + "\n\n【审核建议】\n" + review;

        return new PipelineResult(facts, draft, review, finalAnswer);
    }

    public record PipelineResult(String facts, String draft, String review, String finalAnswer) {}
}
