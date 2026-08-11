package cc.misshi.springai.functioncalling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 工具类 1:时间相关。
 *
 * <p>用 {@code @Tool(description = "...")} 注解,Spring AI 会把方法注册成 LLM 可调用的工具。
 * description 一定要清晰,LLLM 根据 description 决定何时调、调哪个。
 */
public class TimeTools {

    @Tool(description = "Get the current date and time in yyyy-MM-dd HH:mm:ss format")
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "Get the current year as an integer")
    public int getCurrentYear() {
        return LocalDateTime.now().getYear();
    }
}
