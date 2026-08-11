package cc.misshi.springai.agentbasics;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 天气查询工具 — 第 13 章 Agent 基础演示.
 *
 * <p>用 {@code @Tool} 注解把 Java 方法暴露给 LLM,模型会:
 * <ol>
 *   <li>看到方法名 + description 决定何时调用</li>
 *   <li>从用户问题里提取参数</li>
 *   <li>调用方法拿结果</li>
 *   <li>把结果组织成自然语言回复</li>
 * </ol>
 */
@Component
public class WeatherTools {

    /**
     * 模拟天气查询(0 网络,纯本地).
     * 真实项目替换成 Open-Meteo / 和风天气 / 高德天气 API.
     */
    @Tool(description = "查询指定城市的当前天气,返回温度+天气+湿度")
    public WeatherInfo getCurrentWeather(
            @ToolParam(description = "城市名,例如 '北京' / '上海' / '深圳'") String city) {
        // mock 数据:用 hash 决定温度,保证稳定
        int hash = Math.abs(city.hashCode() % 30);
        int temp = 10 + hash;
        String condition = (hash % 3 == 0) ? "晴朗" : (hash % 3 == 1) ? "多云" : "小雨";
        int humidity = 40 + (hash % 40);
        return new WeatherInfo(city, temp, condition, humidity);
    }

    /**
     * 天气信息 record(Java 16+,Spring Boot 4 默认支持).
     */
    public record WeatherInfo(String city, int temperature, String condition, int humidity) {}
}
