package cc.misshi.springai.codereview;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeReviewServiceTest {

    @Test
    void reviewShouldProduceCompleteReport() {
        CodeReviewService service = new CodeReviewService(null, new CodeAnalysisTools());

        String code = """
                // 计算阶乘
                public int factorial(int n) {
                    try {
                        if (n <= 1) return 1;
                        return n * factorial(n - 1);
                    } catch (Exception e) { }
                }
                """;
        var report = service.review(code, "java");

        assertThat(report.language()).isEqualTo("java");
        assertThat(report.lineCount().code()).isGreaterThan(0);
        assertThat(report.antiPatterns()).isNotEmpty();
        assertThat(report.score()).isLessThan(100);
        assertThat(report.semanticReview()).contains("AI 语义审查");
    }

    @Test
    void reviewShouldHandleEmptyCode() {
        CodeReviewService service = new CodeReviewService(null, new CodeAnalysisTools());
        var report = service.review("", "java");
        assertThat(report.lineCount().total()).isZero();
        assertThat(report.antiPatterns()).isEmpty();
        assertThat(report.score()).isEqualTo(100);
    }
}
