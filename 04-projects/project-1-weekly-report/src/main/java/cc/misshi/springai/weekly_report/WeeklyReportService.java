package cc.misshi.springai.weekly_report;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 周报生成服务.
 *
 * <p>用户输入本周完成的工作条目列表 + 下周计划 + 阻塞项,
 * ChatClient 输出 markdown 格式的中文周报.
 *
 * <p>真实 LLM: 调 OpenAI 整理润色.
 * 0 网络: 直接把列表拼成 markdown,验证模板逻辑.
 */
@Service
public class WeeklyReportService {

    private final ChatClient chatClient;

    public WeeklyReportService(ChatClient.Builder builder) {
        this.chatClient = (builder == null) ? null : builder
                .defaultSystem("""
                        你是一个 helpful 的助理,负责把工程师的工作条目整理成结构化的中文周报。
                        风格:简洁、客观、突出价值。
                        输出:Markdown 格式,包含「本周完成」「下周计划」「风险与阻塞」「数据指标」4 个 H2 段落。
                        """)
                .build();
    }

    /**
     * 生成周报 — 真实 LLM(若 chatClient 已注入) 或 0 网络 mock.
     */
    public String generate(List<String> completed, List<String> planned, List<String> blockers) {
        String userPrompt = buildUserPrompt(completed, planned, blockers);
        // 真实场景: chatClient.prompt().user(userPrompt).call().content();
        // 0 网络 fallback: 返回组装好的 mock 周报
        if (chatClient == null) {
            return buildMockReport(completed, planned, blockers);
        }
        return chatClient.prompt().user(userPrompt).call().content();
    }

    private String buildUserPrompt(List<String> completed, List<String> planned, List<String> blockers) {
        return """
                【本周完成】
                %s

                【下周计划】
                %s

                【风险与阻塞】
                %s
                """.formatted(
                String.join("\n- ", completed),
                String.join("\n- ", planned),
                String.join("\n- ", blockers)
        );
    }

    private String buildMockReport(List<String> completed, List<String> planned, List<String> blockers) {
        return """
                # 周报 · %s

                ## 本周完成
                %s

                ## 下周计划
                %s

                ## 风险与阻塞
                %s

                ## 数据指标
                - 本周完成: %d 项
                - 下周计划: %d 项
                - 当前阻塞: %d 项
                """.formatted(
                java.time.LocalDate.now(),
                String.join("\n- ", completed),
                String.join("\n- ", planned),
                String.join("\n- ", blockers),
                completed.size(),
                planned.size(),
                blockers.size()
        );
    }
}
