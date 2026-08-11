package cc.misshi.springai.knowledgehub;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码片段分析 — 简化版 @Tool,提供给 hub 在 CODE_REVIEW 路由时调用.
 */
@Component
public class CodeSnippetAnalyzer {

    public CodeReviewSummary review(String code) {
        if (code == null || code.isBlank()) {
            return new CodeReviewSummary(0, 0, List.of("代码为空"));
        }
        int totalLines = code.split("\n").length;
        int complexity = countKeywords(code, new String[]{"if", "else if", "while", "for", "case", "&&", "||"}) + 1;

        List<String> issues = new ArrayList<>();
        if (code.contains("System.out.println")) issues.add("使用 System.out.println,应改 logger");
        if (code.contains("password") || code.contains("Password")) issues.add("疑似硬编码密码");
        if (code.contains("catch (") && code.contains(") { }")) issues.add("空 catch 块");
        if (code.contains("TODO") || code.contains("FIXME")) issues.add("存在 TODO / FIXME");
        if (code.length() / Math.max(1, totalLines) > 200) issues.add("存在超长行");

        return new CodeReviewSummary(totalLines, complexity, issues);
    }

    private static int countKeywords(String text, String[] keywords) {
        int count = 0;
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = text.indexOf(kw, idx)) != -1) {
                count++;
                idx += kw.length();
            }
        }
        return count;
    }

    public record CodeReviewSummary(int lines, int complexity, List<String> issues) {}
}
