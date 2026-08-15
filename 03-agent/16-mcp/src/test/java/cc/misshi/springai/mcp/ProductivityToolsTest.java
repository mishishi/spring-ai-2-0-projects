package cc.misshi.springai.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductivityToolsTest {

    private final ProductivityTools tools = new ProductivityTools();

    @Test
    void createTaskShouldReturnTaskId() {
        var result = tools.createTask("写文档", "Phase 3 README");
        assertThat(result).contains("任务已创建").contains("写文档");
    }

    @Test
    void getCalendarEventsShouldReturnMockEvents() {
        var events = tools.getCalendarEvents("2026-08-12");
        assertThat(events).hasSize(3);
        assertThat(events.get(0)).containsKey("time").containsKey("title");
    }

    @Test
    void sendEmailShouldReturnStatus() {
        var result = tools.sendEmail("a@b.com", "hi", "body");
        assertThat(result).contains("a@b.com").contains("hi").contains("长度=4");
    }

    @Test
    void convertUnitCelsiusToFahrenheit() {
        var result = tools.convertUnit(100, "celsius", "fahrenheit");
        assertThat(result).contains("100.00°C = 212.00°F");
    }

    @Test
    void convertUnitKmToMile() {
        var result = tools.convertUnit(10, "km", "mile");
        assertThat(result).contains("10.00 km = 6.21 mile");
    }

    @Test
    void convertUnitKgToLb() {
        var result = tools.convertUnit(1, "kg", "lb");
        assertThat(result).contains("1.00 kg = 2.20 lb");
    }

    @Test
    void convertUnitUnknownShouldReturnErrorMessage() {
        var result = tools.convertUnit(1, "unknown", "also-unknown");
        assertThat(result).startsWith("不支持的转换");
    }
}
