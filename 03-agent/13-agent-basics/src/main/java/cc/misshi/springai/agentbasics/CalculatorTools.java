package cc.misshi.springai.agentbasics;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 计算器工具 — 第 13 章 Agent 基础演示.
 *
 * <p>展示 @Tool + @ToolParam 的常见模式:
 * <ul>
 *   <li>多个 @Tool 方法可以同 class 注册</li>
 *   <li>每个方法独立 description 决定调用场景</li>
 *   <li>@ToolParam 给每个参数加描述,帮助模型理解</li>
 * </ul>
 */
@Component
public class CalculatorTools {

    @Tool(description = "把两个数字相加")
    public double add(
            @ToolParam(description = "第一个数") double a,
            @ToolParam(description = "第二个数") double b) {
        return a + b;
    }

    @Tool(description = "把第一个数减去第二个数")
    public double subtract(
            @ToolParam(description = "被减数") double a,
            @ToolParam(description = "减数") double b) {
        return a - b;
    }

    @Tool(description = "计算两个数的乘积")
    public double multiply(
            @ToolParam(description = "第一个数") double a,
            @ToolParam(description = "第二个数") double b) {
        return a * b;
    }

    @Tool(description = "计算第一个数除以第二个数,被除数为 0 时返回错误字符串")
    public String divide(
            @ToolParam(description = "被除数") double a,
            @ToolParam(description = "除数") double b) {
        if (b == 0) {
            return "错误:除数不能为 0";
        }
        return String.valueOf(a / b);
    }
}
