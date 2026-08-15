package cc.misshi.springai.multiagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 3 个 sub-agent + 1 个 orchestrator — 第 16 章 Multi-Agent 编排.
 *
 * <p>模式: Orchestrator-Workers (Anthropic 风格)
 * <ol>
 *   <li>Orchestrator 分析用户请求,决定调哪些 sub-agent</li>
 *   <li>Sub-agent 各司其职 (Researcher / Writer / Reviewer)</li>
 *   <li>Orchestrator 整合结果返回</li>
 * </ol>
 */
@Configuration
public class AgentsConfig {

    @Bean(name = "researcherAgent")
    public ChatClient researcherAgent(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是 Researcher(研究员)。
                        职责: 收集信息、列举事实、引用来源(可 mock)。
                        输出格式: 简洁的事实清单,每条 1 行。
                        """)
                .build();
    }

    @Bean(name = "writerAgent")
    public ChatClient writerAgent(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是 Writer(写作员)。
                        职责: 把事实信息组织成流畅的中文段落。
                        风格: 简洁,不超过 200 字。
                        """)
                .build();
    }

    @Bean(name = "reviewerAgent")
    public ChatClient reviewerAgent(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是 Reviewer(审核员)。
                        职责: 检查草稿的事实准确性、语气、流畅度,给出修改建议。
                        输出: 3 条以内的 bullet list 反馈。
                        """)
                .build();
    }

    /**
     * Orchestrator 用同一个 Builder,但 defaultSystem 描述它怎么协调 3 个 sub-agent.
     * 真实 LLM 调 sub-agent 工具,但 0 网络测试只验证 Bean 装配.
     */
    @Bean(name = "orchestratorAgent")
    public ChatClient orchestratorAgent(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是 Orchestrator(协调员)。
                        接到用户请求后,你会:
                        1. 调 researcherAgent 收集事实
                        2. 调 writerAgent 写初稿
                        3. 调 reviewerAgent 审核
                        4. 整合最终结果给用户
                        """)
                .build();
    }
}
