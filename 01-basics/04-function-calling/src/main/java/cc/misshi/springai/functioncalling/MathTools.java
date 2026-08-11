package cc.misshi.springai.functioncalling;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 工具类 2:数学计算。
 *
 * <p>参数必须用 {@code @ToolParam(description = "...")} 标注,LLM 才能知道参数含义。
 * 不标注 description,LLLM 会瞎猜。
 */
public class MathTools {

    @Tool(description = "Add two numbers and return the sum")
    public int add(
            @ToolParam(description = "The first number") int a,
            @ToolParam(description = "The second number") int b) {
        return a + b;
    }

    @Tool(description = "Multiply two numbers and return the product")
    public int multiply(
            @ToolParam(description = "The first number") int a,
            @ToolParam(description = "The second number") int b) {
        return a * b;
    }
}
