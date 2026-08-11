package cc.misshi.springai.multiagent;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/multi-agent")
public class MultiAgentController {

    private final OrchestrationService orchestration;

    public MultiAgentController(OrchestrationService orchestration) {
        this.orchestration = orchestration;
    }

    /**
     * POST /multi-agent/run
     * body: { "request": "写一篇关于 Spring AI 2.0 的短文" }
     *
     * 0 网络时 facts 是 mock,真实跑由 orchestrator ChatClient 串接.
     */
    @PostMapping("/run")
    public OrchestrationService.PipelineResult run(
            @RequestBody RunRequest body) {
        // mock facts(0 网络)
        String mockedFacts = """
                1. Spring AI 2.0 GA 2026-06 发布
                2. 内置 MCP SDK 2.0
                3. Streamable HTTP 成为默认 transport
                4. @Tool 注解 + MethodToolCallbackProvider
                5. MemoryAdvisor / ChatMemory 内置
                """;
        return orchestration.runPipeline(body.request(), mockedFacts);
    }

    public record RunRequest(String request) {}
}
