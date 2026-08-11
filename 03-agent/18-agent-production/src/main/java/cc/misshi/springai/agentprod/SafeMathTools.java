package cc.misshi.springai.agentprod;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 安全工具 — 第 18 章.
 *
 * <p>展示 3 个生产化安全考虑:
 * <ol>
 *   <li>输入验证(regex 防注入)</li>
 *   <li>白名单(只允许特定操作)</li>
 *   <li>错误处理(不让异常冒泡到模型)</li>
 * </ol>
 */
@Component
public class SafeMathTools {

    // 只允许整数 / 小数 / 负数
    private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    @Tool(description = "安全加法:输入必须是数字,否则返回错误字符串")
    public String safeAdd(
            @ToolParam(description = "第一个数,必须匹配 -?\\d+(\\.\\d+)?") String a,
            @ToolParam(description = "第二个数,必须匹配 -?\\d+(\\.\\d+)?") String b) {
        if (!NUMERIC.matcher(a).matches() || !NUMERIC.matcher(b).matches()) {
            return "错误: 输入必须是数字,得到 a='" + a + "', b='" + b + "'";
        }
        return String.valueOf(Double.parseDouble(a) + Double.parseDouble(b));
    }

    @Tool(description = "白名单字符串反转:输入必须在白名单内(只接受 a-z 字符)")
    public String whitelistReverse(
            @ToolParam(description = "字符串,只接受 a-zA-Z") String input) {
        if (input == null || !input.matches("[a-zA-Z]+")) {
            return "错误: 只能反转英文字符串";
        }
        return new StringBuilder(input).reverse().toString();
    }
}
