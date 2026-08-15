package cc.misshi.springai.agentprod;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SafeMathToolsTest {

    private final SafeMathTools tools = new SafeMathTools();

    @Test
    void safeAddValidInput() {
        assertThat(tools.safeAdd("2", "3")).isEqualTo("5.0");
        assertThat(tools.safeAdd("-1.5", "1.5")).isEqualTo("0.0");
    }

    @Test
    void safeAddInvalidInput() {
        assertThat(tools.safeAdd("abc", "3")).startsWith("错误: 输入必须是数字");
        assertThat(tools.safeAdd("2", "")).startsWith("错误: 输入必须是数字");
    }

    @Test
    void whitelistReverseValidInput() {
        assertThat(tools.whitelistReverse("hello")).isEqualTo("olleh");
    }

    @Test
    void whitelistReverseInvalidInput() {
        assertThat(tools.whitelistReverse("hello123")).startsWith("错误");
        assertThat(tools.whitelistReverse("中文")).startsWith("错误");
    }
}
