package cc.misshi.springai.codereview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code Analysis Tools — 作为 @Tool 给 ChatClient 调用.
 *
 * <p>Phase 4 项目 4 的核心:AI Agent 用这些工具做静态代码分析.
 * <p>0 网络: 全部本地计算,无外部依赖.
 */
@Component
public class CodeAnalysisTools {

    private static final Logger log = LoggerFactory.getLogger(CodeAnalysisTools.class);

    /**
     * 工具 1: 计算代码行数 / 空行 / 注释行.
     */
    @Tool(description = "统计代码行数,返回总行数/空行数/注释行数/代码行数")
    public LineCountResult countLines(String code, String language) {
        log.info("countLines: language={}, length={}", language, code == null ? 0 : code.length());
        if (code == null || code.isEmpty()) {
            return new LineCountResult(0, 0, 0, 0);
        }
        String[] lines = code.split("\n");
        int total = lines.length;
        int blank = 0, comment = 0;
        String commentPrefix = switch (language == null ? "" : language.toLowerCase()) {
            case "java", "javascript", "js", "go", "kotlin" -> "//";
            case "python", "py", "ruby", "shell", "bash" -> "#";
            default -> null;
        };
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) blank++;
            else if (commentPrefix != null && trimmed.startsWith(commentPrefix)) comment++;
        }
        int codeLines = total - blank - comment;
        return new LineCountResult(total, blank, comment, codeLines);
    }

    /**
     * 工具 2: 检测常见反模式 / 危险代码.
     */
    @Tool(description = "检测代码中的常见反模式(空 catch / System.out / 硬编码密码 / SQL 拼接 / TODO 等)")
    public List<AntiPatternHit> detectAntiPatterns(String code, String language) {
        log.info("detectAntiPatterns: language={}, length={}", language, code == null ? 0 : code.length());
        List<AntiPatternHit> hits = new ArrayList<>();
        if (code == null || code.isEmpty()) return hits;

        // 1. 空 catch
        Pattern emptyCatch = Pattern.compile("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}");
        if (emptyCatch.matcher(code).find()) {
            hits.add(new AntiPatternHit("空 catch 块", "high", "至少 log.error(...) 或重新抛出"));
        }

        // 2. System.out.println (Java/Kotlin/JS)
        if (code.contains("System.out.println")) {
            hits.add(new AntiPatternHit("System.out.println", "low", "生产代码应改用 logger"));
        }
        if (code.contains("console.log")) {
            hits.add(new AntiPatternHit("console.log", "low", "生产代码应改用 logger"));
        }

        // 3. 硬编码密码
        Pattern passwordPattern = Pattern.compile("(?i)(password|passwd|pwd)\\s*=\\s*[\"']([^\"']+)[\"']");
        Matcher m = passwordPattern.matcher(code);
        if (m.find()) {
            hits.add(new AntiPatternHit("硬编码密码: " + m.group(2), "high", "应使用环境变量 / 配置中心"));
        }

        // 4. SQL 拼接
        if (code.matches("(?s).*[\"']\\s*\\+\\s*[a-zA-Z_].*\\s*\\+\\s*[\"']\\s*SELECT.*") ||
            code.contains("Statement ") && code.contains("executeQuery")) {
            hits.add(new AntiPatternHit("可能存在 SQL 拼接", "high", "应使用 PreparedStatement / 参数化查询"));
        }

        // 5. TODO / FIXME
        if (code.contains("TODO") || code.contains("FIXME")) {
            hits.add(new AntiPatternHit("TODO / FIXME", "low", "提交前应清理或转 issue"));
        }

        // 6. 过长行(>200 字符)
        for (String line : code.split("\n")) {
            if (line.length() > 200) {
                hits.add(new AntiPatternHit("超长行(" + line.length() + " 字符)", "low", "建议拆行"));
                break;
            }
        }

        // 7. 深度嵌套(>4 层)
        int maxDepth = computeMaxNestingDepth(code);
        if (maxDepth > 4) {
            hits.add(new AntiPatternHit("嵌套深度 " + maxDepth, "medium", "建议抽取方法 / 早返回"));
        }

        return hits;
    }

    /**
     * 工具 3: 估算圈复杂度.
     */
    @Tool(description = "估算代码的圈复杂度(McCabe): if/while/for/case/catch/and/or 各 +1")
    public ComplexityResult estimateComplexity(String code) {
        log.info("estimateComplexity: length={}", code == null ? 0 : code.length());
        if (code == null || code.isEmpty()) return new ComplexityResult(0, "simple");

        int complexity = 1; // baseline
        // 简单计数关键字
        String[] keywords = {"if", "else if", "while", "for", "case", "catch", "&&", "||", "?"};
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = code.indexOf(kw, idx)) != -1) {
                complexity++;
                idx += kw.length();
            }
        }

        String level;
        if (complexity <= 5) level = "simple";
        else if (complexity <= 10) level = "moderate";
        else if (complexity <= 20) level = "complex";
        else level = "untestable";
        return new ComplexityResult(complexity, level);
    }

    private int computeMaxNestingDepth(String code) {
        int max = 0, current = 0;
        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            if (ch == '{') {
                current++;
                if (current > max) max = current;
            } else if (ch == '}') {
                current--;
            }
        }
        return max;
    }

    // ─── DTO ───────────────────────────────────────────

    public record LineCountResult(int total, int blank, int comment, int code) {}

    public record AntiPatternHit(String name, String severity, String suggestion) {}

    public record ComplexityResult(int complexity, String level) {}
}
