package cc.misshi.springai.toolcalling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第 14 章 · Tool Calling 深入 入口.
 *
 * <p>覆盖 5 大核心特性:
 * <ol>
 *   <li>@Tool + @ToolParam 注解</li>
 *   <li>@ToolParam(required = false) 可选参数</li>
 *   <li>returnDirect — 工具结果直接返回不经过模型</li>
 *   <li>ToolContext — 给工具传额外上下文(用户 ID / 租户 ID)</li>
 *   <li>FunctionToolCallback — 函数式工具(比 @Bean + @Description 强类型)</li>
 * </ol>
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
