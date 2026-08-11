package cc.misshi.springai.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 暴露的工具集.
 *
 * <p>Spring AI 2.0 + spring-ai-starter-mcp-server-webmvc 自动把这些 @Tool 方法
 * 注册到 MCP 协议的 /sse(或 /mcp/messages) 端点.
 * 任何支持 MCP 的客户端(Claude Desktop / 其他 AI 应用 / 我们自己的 Client)都能发现并调用.
 */
@Component
public class ProductivityTools {

    /**
     * 工具 1: 任务管理.
     */
    @Tool(description = "创建一条新任务,返回任务 ID")
    public String createTask(
            @ToolParam(description = "任务标题") String title,
            @ToolParam(description = "任务描述") String description) {
        // mock:用 hash 生成 ID
        long id = Math.abs((title + description).hashCode() % 100000);
        return String.format("任务已创建: ID=%d, 标题='%s'", id, title);
    }

    /**
     * 工具 2: 日历查询.
     */
    @Tool(description = "查询指定日期的日历事件(0 网络 mock)")
    public List<Map<String, String>> getCalendarEvents(
            @ToolParam(description = "日期,格式 YYYY-MM-DD") String date) {
        // mock 数据
        return List.of(
                Map.of("time", "09:00", "title", "晨会", "location", "会议室 A"),
                Map.of("time", "14:00", "title", "客户对接", "location", "Zoom"),
                Map.of("time", "16:30", "title", "代码 review", "location", "线上")
        );
    }

    /**
     * 工具 3: 邮件发送(0 网络 mock).
     */
    @Tool(description = "给指定收件人发送邮件,返回发送状态")
    public String sendEmail(
            @ToolParam(description = "收件人邮箱") String to,
            @ToolParam(description = "邮件主题") String subject,
            @ToolParam(description = "邮件正文") String body) {
        return String.format("邮件已发送到 %s: 主题='%s', 正文长度=%d", to, subject, body.length());
    }

    /**
     * 工具 4: 单位换算(纯计算,无网络).
     */
    @Tool(description = "把数值从一种单位转换到另一种,支持 temperature/length/weight")
    public String convertUnit(
            @ToolParam(description = "数值") double value,
            @ToolParam(description = "源单位,例如 celsius / km / kg") String fromUnit,
            @ToolParam(description = "目标单位,例如 fahrenheit / mile / lb") String toUnit) {
        if ("celsius".equals(fromUnit) && "fahrenheit".equals(toUnit)) {
            return String.format("%.2f°C = %.2f°F", value, value * 9 / 5 + 32);
        }
        if ("km".equals(fromUnit) && "mile".equals(toUnit)) {
            return String.format("%.2f km = %.2f mile", value, value * 0.621371);
        }
        if ("kg".equals(fromUnit) && "lb".equals(toUnit)) {
            return String.format("%.2f kg = %.2f lb", value, value * 2.20462);
        }
        return "不支持的转换: " + fromUnit + " → " + toUnit;
    }
}
