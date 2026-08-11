package cc.misshi.springai.toolcalling;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Tool 注册配置 — 第 14 章.
 *
 * <p>演示 3 种工具注册方式:
 * <ol>
 *   <li>Method 工具:CustomerTools 上的 @Tool 注解(通过 MethodToolCallbackProvider)</li>
 *   <li>Function 工具:Java Function 接口(通过 FunctionToolCallback.builder())</li>
 *   <li>动态解析:Spring Bean + @Description(本例不演示,在 17-graph 章节看)</li>
 * </ol>
 */
@Configuration
public class ToolConfig {

    /**
     * 自动扫描 CustomerTools 上所有 @Tool 注解.
     */
    @Bean
    public ToolCallbackProvider methodTools(CustomerTools customerTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(customerTools)
                .build();
    }

    /**
     * 用 Java 17 Function 接口定义一个工具(完全编程式,不需要 @Bean).
     * <p>适用场景:工具逻辑短/动态/不想注册为 Spring Bean.
     */
    @Bean
    public FunctionToolCallback<String, String> reverseStringTool() {
        return FunctionToolCallback.builder("reverseString",
                        (Function<String, String>) input -> new StringBuilder(input).reverse().toString())
                .description("把字符串反转,例如 'hello' → 'olleh'")
                .inputType(String.class)
                .build();
    }
}
