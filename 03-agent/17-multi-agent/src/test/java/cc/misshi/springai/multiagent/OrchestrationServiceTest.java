package cc.misshi.springai.multiagent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Multi-Agent 0 网络测试 — 验证 4 个 ChatClient Bean 装配 + 编排流程.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.ai.openai.api-key=fake-key")
class OrchestrationServiceTest {

    @Autowired OrchestrationService orchestration;
    @Autowired @Qualifier("researcherAgent") Object researcher;
    @Autowired @Qualifier("writerAgent") Object writer;
    @Autowired @Qualifier("reviewerAgent") Object reviewer;
    @Autowired @Qualifier("orchestratorAgent") Object orchestrator;

    @Test
    void allFourAgentsShouldBeAutowired() {
        assertThat(researcher).isNotNull();
        assertThat(writer).isNotNull();
        assertThat(reviewer).isNotNull();
        assertThat(orchestrator).isNotNull();
    }

    @Test
    void pipelineShouldProduceAllStages() {
        var result = orchestration.runPipeline(
                "写一篇关于 Spring AI 2.0 的短文",
                "Spring AI 2.0 GA 发布,内置 MCP,支持 Tool Calling"
        );

        assertThat(result.facts()).contains("Spring AI 2.0");
        assertThat(result.draft()).contains("MOCK DRAFT");
        assertThat(result.review()).contains("MOCK REVIEW");
        assertThat(result.finalAnswer()).contains("【研究报告】").contains("【审核建议】");
    }
}
