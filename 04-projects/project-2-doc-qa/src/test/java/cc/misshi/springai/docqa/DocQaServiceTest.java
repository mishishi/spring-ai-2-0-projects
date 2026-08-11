package cc.misshi.springai.docqa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocQaService 单元测试 — 0 网络, 0 mockito.
 */
class DocQaServiceTest {

    private DocQaService service;

    @BeforeEach
    void setup() throws IOException {
        // 准备 2 段文档
        String md = """
                报销流程: 提交 OA 申请,主管审批,财务打款。7 天内提交。

                加班制度: 周末加班 2 倍工资。
                """;
        Resource doc = new ByteArrayResource(md.getBytes(StandardCharsets.UTF_8)) {
            @Override public String getFilename() { return "handbook.md"; }
        };
        // 用 null builder 触发 mock 路径
        service = new DocQaService(null, new Resource[]{doc});
        service.loadDocsOnStartup();
    }

    @Test
    void askShouldReturnFallbackWhenNoMatch() {
        String answer = service.ask("天气怎么样?");
        assertThat(answer).contains("我不知道");
    }

    @Test
    void askShouldHitReimbursementDoc() {
        String answer = service.ask("怎么报销?");
        assertThat(answer).contains("命中 1 个文档段落");
        assertThat(answer).contains("报销");
    }

    @Test
    void askShouldHitOvertimeDoc() {
        String answer = service.ask("周末加班工资?");
        assertThat(answer).contains("加班");
    }

    @Test
    void askShouldHandleEmptyQuestion() {
        assertThat(service.ask("")).contains("问题不能为空");
        assertThat(service.ask(null)).contains("问题不能为空");
    }

    @Test
    void searchShouldReturnTop3() {
        var hits = service.search("报销 加班", 3);
        assertThat(hits).isNotEmpty();
        assertThat(hits.size()).isLessThanOrEqualTo(3);
    }
}
