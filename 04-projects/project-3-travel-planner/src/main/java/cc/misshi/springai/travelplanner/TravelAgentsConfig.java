package cc.misshi.springai.travelplanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 4 个 sub-agent + 1 个 orchestrator — Phase 4 项目 3.
 *
 * <p>每个 agent 有自己的 system prompt 角色, 真实 LLM 时各司其职.
 * <p>0 网络测试只验证 Bean 装配,实际 sub-agent 内容由 mock 模板生成.
 */
@Configuration
public class TravelAgentsConfig {

    @Bean(name = "destinationAgent")
    public ChatClient destinationAgent(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是 DestinationAgent(目的地顾问)。
                        职责: 根据用户的兴趣/季节/预算,推荐 3 个候选目的地。
                        每个目的地给出: 名称、最佳季节、3 个亮点。
                        输出格式: markdown bullet list。
                        """)
                .build();
    }

    @Bean(name = "itineraryAgent")
    public ChatClient itineraryAgent(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是 ItineraryAgent(行程规划师)。
                        职责: 把目的地拆成 N 天行程,每天上午/下午/晚上各 1 个活动。
                        输出格式: 按天分小节,markdown 列表。
                        """)
                .build();
    }

    @Bean(name = "budgetAgent")
    public ChatClient budgetAgent(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是 BudgetAgent(预算分析师)。
                        职责: 估算机票/酒店/餐饮/门票/交通费用,给出总预算区间。
                        输出格式: 表格(类别/单日/小计)。
                        """)
                .build();
    }

    @Bean(name = "bookingAgent")
    public ChatClient bookingAgent(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是 BookingAgent(票务顾问)。
                        职责: 推荐订票时机(提前多久/哪个平台/有无优惠)。
                        输出格式: 简洁的 checklist。
                        """)
                .build();
    }

    @Bean(name = "travelOrchestrator")
    public ChatClient travelOrchestrator(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        你是 TravelOrchestrator(旅行总协调)。
                        接到请求后,你会:
                        1. 调 destinationAgent 选 3 个候选目的地
                        2. 调 itineraryAgent 编排行程
                        3. 调 budgetAgent 估算预算
                        4. 调 bookingAgent 推荐订票策略
                        5. 整合为一份完整的旅行计划 markdown
                        """)
                .build();
    }
}
