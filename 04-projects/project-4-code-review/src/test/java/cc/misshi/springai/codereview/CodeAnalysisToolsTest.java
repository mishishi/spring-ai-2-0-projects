package cc.misshi.springai.codereview;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeAnalysisToolsTest {

    private final CodeAnalysisTools tools = new CodeAnalysisTools();

    @Test
    void countLinesShouldExcludeBlankAndComment() {
        String code = """
                // 注释 1
                public class A {

                    public void m() {
                        int x = 1;

                    }
                }
                """;
        var r = tools.countLines(code, "java");
        assertThat(r.total()).isEqualTo(8);
        assertThat(r.blank()).isEqualTo(2);
        assertThat(r.comment()).isEqualTo(1);
        assertThat(r.code()).isEqualTo(5);
    }

    @Test
    void detectAntiPatternsShouldFindEmptyCatch() {
        String code = """
                public void m() {
                    try { doIt(); } catch (Exception e) { }
                }
                """;
        var hits = tools.detectAntiPatterns(code, "java");
        assertThat(hits).anyMatch(h -> h.name().contains("空 catch"));
    }

    @Test
    void detectAntiPatternsShouldFindHardcodedPassword() {
        String code = "String password = \"mySecret123\";";
        var hits = tools.detectAntiPatterns(code, "java");
        assertThat(hits).anyMatch(h -> h.name().contains("硬编码密码"));
    }

    @Test
    void detectAntiPatternsShouldFindSystemOut() {
        String code = "public void m() { System.out.println(\"hi\"); }";
        var hits = tools.detectAntiPatterns(code, "java");
        assertThat(hits).anyMatch(h -> h.name().contains("System.out"));
    }

    @Test
    void detectAntiPatternsShouldFindDeepNesting() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append("{\n");
        for (int i = 0; i < 6; i++) sb.append("}\n");
        var hits = tools.detectAntiPatterns(sb.toString(), "java");
        assertThat(hits).anyMatch(h -> h.name().contains("嵌套深度"));
    }

    @Test
    void estimateComplexityShouldCountBranches() {
        String code = """
                if (a) {
                    while (b) {
                        for (int i = 0; i < 10; i++) {
                            if (c && d) {
                                doIt();
                            }
                        }
                    }
                }
                """;
        var r = tools.estimateComplexity(code);
        assertThat(r.complexity()).isGreaterThan(5);
    }

    @Test
    void emptyCodeShouldReturnEmptyResults() {
        var lc = tools.countLines("", "java");
        assertThat(lc.total()).isZero();

        var hits = tools.detectAntiPatterns(null, "java");
        assertThat(hits).isEmpty();

        var cx = tools.estimateComplexity(null);
        assertThat(cx.complexity()).isZero();
    }
}
