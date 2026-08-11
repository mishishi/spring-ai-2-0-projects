package cc.misshi.springai.agentbasics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 第 13 章 · Agent 基础 入口.
 *
 * <p>展示 Spring AI 2.0 Agent loop 的核心组成:
 * <ul>
 *   <li>{@link ChatClient} — 统一对话入口</li>
 *   <li>{@code @Tool} 注解 — 把 Java 方法暴露给模型</li>
 *   <li>{@code MethodToolCallbackProvider} — 自动扫描 + 注册工具</li>
 *   <li>Spring Boot 自动配置 — 0 配置启动</li>
 * </ul>
 *
 * <p>跑起来: <code>export OPENAI_API_KEY=sk-xxxxx && mvn spring-boot:run</code>
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 把所有 @Tool 注解的方法统一注册成 ToolCallback.
     * Spring AI 启动时自动注入到 ChatClient 的 defaultTools 里.
     */
    @Bean
    public org.springframework.ai.tool.ToolCallbackProvider toolCallbackProvider(
            WeatherTools weatherTools,
            CalculatorTools calculatorTools) {
        return org.springframework.ai.tool.method.MethodToolCallbackProvider.builder()
                .toolObjects(weatherTools, calculatorTools)
                .build();
    }
}
