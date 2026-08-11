package cc.misshi.springai.codereview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Code Review Service — 协调 @Tool 做静态检查 + ChatClient 做语义审查.
 *
 * <p>0 网络: tool 调用是本地计算, ChatClient 为 null 时跳过语义审查, 走 mock 模板.
 */
@Service
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    private final ChatClient chatClient;
    private final CodeAnalysisTools tools;

    public CodeReviewService(@Autowired(required = false) ChatClient.Builder builder,
                             CodeAnalysisTools tools) {
        this.chatClient = (builder == null) ? null : builder
                .defaultSystem("""
                        你是 Code Review 专家。
                        收到代码后,基于静态检查工具的结果,给出综合审查报告:
                        - 总结:代码质量打分(0-100)
                        - 必须修改:高危问题列表
                        - 建议改进:中低优问题列表
                        - 亮点:做得好的地方
                        """)
                .build();
        this.tools = tools;
    }

    /**
     * 审查代码:工具静态分析 + (可选)LLM 语义审查.
     */
    public ReviewReport review(String code, String language) {
        log.info("审查代码: language={}, length={}", language, code == null ? 0 : code.length());

        // 1. 静态分析(@Tool 调用)
        var lineCount = tools.countLines(code, language);
        var antiPatterns = tools.detectAntiPatterns(code, language);
        var complexity = tools.estimateComplexity(code);

        // 2. 语义审查(0 网络时跳过,真实 LLM 时启用)
        String semanticReview;
        if (chatClient == null) {
            semanticReview = mockSemanticReview(antiPatterns, complexity, lineCount);
        } else {
            String prompt = "代码:\n```%s\n%s\n```\n\n静态检查结果:\n%s"
                    .formatted(language, code, formatStaticChecks(antiPatterns, complexity, lineCount));
            semanticReview = chatClient.prompt().user(prompt).call().content();
        }

        // 3. 综合评分
        int score = computeScore(antiPatterns, complexity, lineCount);

        return new ReviewReport(
                language,
                lineCount,
                antiPatterns,
                complexity,
                semanticReview,
                score
        );
    }

    private int computeScore(List<CodeAnalysisTools.AntiPatternHit> hits,
                             CodeAnalysisTools.ComplexityResult complexity,
                             CodeAnalysisTools.LineCountResult lineCount) {
        int score = 100;
        for (var h : hits) {
            score -= switch (h.severity()) {
                case "high" -> 15;
                case "medium" -> 8;
                case "low" -> 3;
                default -> 1;
            };
        }
        if ("complex".equals(complexity.level())) score -= 10;
        if ("untestable".equals(complexity.level())) score -= 20;
        return Math.max(0, score);
    }

    private String formatStaticChecks(List<CodeAnalysisTools.AntiPatternHit> hits,
                                      CodeAnalysisTools.ComplexityResult cx,
                                      CodeAnalysisTools.LineCountResult lc) {
        StringBuilder sb = new StringBuilder();
        sb.append("- 总行数: ").append(lc.total())
          .append(" (代码 ").append(lc.code()).append(" / 空行 ").append(lc.blank())
          .append(" / 注释 ").append(lc.comment()).append(")\n");
        sb.append("- 圈复杂度: ").append(cx.complexity()).append(" (").append(cx.level()).append(")\n");
        if (hits.isEmpty()) {
            sb.append("- 反模式: 无\n");
        } else {
            sb.append("- 反模式: ").append(hits.size()).append(" 个\n");
            for (var h : hits) {
                sb.append("  - [").append(h.severity()).append("] ").append(h.name())
                  .append(" → ").append(h.suggestion()).append("\n");
            }
        }
        return sb.toString();
    }

    private String mockSemanticReview(List<CodeAnalysisTools.AntiPatternHit> hits,
                                       CodeAnalysisTools.ComplexityResult cx,
                                       CodeAnalysisTools.LineCountResult lc) {
        StringBuilder sb = new StringBuilder();
        sb.append("【AI 语义审查 · Mock】\n\n");
        sb.append("📊 综合评分: ").append(computeScore(hits, cx, lc)).append("/100\n\n");

        long high = hits.stream().filter(h -> "high".equals(h.severity())).count();
        long medium = hits.stream().filter(h -> "medium".equals(h.severity())).count();
        long low = hits.stream().filter(h -> "low".equals(h.severity())).count();

        if (high == 0 && medium == 0) {
            sb.append("✅ **必须修改**: 无重大问题\n\n");
        } else {
            sb.append("⚠️ **必须修改** (").append(high).append(" 项高危):\n");
            hits.stream().filter(h -> "high".equals(h.severity())).forEach(h ->
                    sb.append("  - ").append(h.name()).append("\n"));
            sb.append("\n");
        }
        if (low + medium > 0) {
            sb.append("💡 **建议改进** (").append(low + medium).append(" 项):\n");
            hits.stream().filter(h -> !"high".equals(h.severity())).forEach(h ->
                    sb.append("  - [").append(h.severity()).append("] ").append(h.name()).append("\n"));
            sb.append("\n");
        }
        sb.append("🎯 **亮点**:\n");
        if (lc.comment() > lc.code() * 0.2) sb.append("  - 注释比例 ").append(String.format("%.1f%%", 100.0 * lc.comment() / Math.max(1, lc.total()))).append(", 文档良好\n");
        if (hits.isEmpty()) sb.append("  - 静态扫描全过, 代码风格干净\n");
        if ("simple".equals(cx.level())) sb.append("  - 圈复杂度低, 易于理解和测试\n");
        return sb.toString();
    }

    public record ReviewReport(
            String language,
            CodeAnalysisTools.LineCountResult lineCount,
            List<CodeAnalysisTools.AntiPatternHit> antiPatterns,
            CodeAnalysisTools.ComplexityResult complexity,
            String semanticReview,
            int score
    ) {}
}
