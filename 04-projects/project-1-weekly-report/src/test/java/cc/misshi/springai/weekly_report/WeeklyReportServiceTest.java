package cc.misshi.springai.weekly_report;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyReportServiceTest {

    private final WeeklyReportService service = new WeeklyReportService(null);

    @Test
    void reportShouldContainAllThreeSections() {
        var report = service.generate(
                List.of("完成 Phase 1", "完成 Phase 2"),
                List.of("完成 Phase 3", "完成 Phase 4"),
                List.of("等待 LLM 反馈")
        );

        assertThat(report).contains("# 周报");
        assertThat(report).contains("## 本周完成").contains("## 下周计划").contains("## 风险与阻塞");
        assertThat(report).contains("完成 Phase 1").contains("完成 Phase 3").contains("等待 LLM 反馈");
    }

    @Test
    void reportShouldReflectItemCounts() {
        var report = service.generate(
                List.of("a", "b", "c"),
                List.of("d"),
                List.of()
        );

        assertThat(report).contains("本周完成: 3 项").contains("下周计划: 1 项").contains("当前阻塞: 0 项");
    }
}
