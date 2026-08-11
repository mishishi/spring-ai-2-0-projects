package cc.misshi.springai.agentbasics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CalculatorTools 单元测试 — 验证基础算术 + 边界.
 */
class CalculatorToolsTest {

    private final CalculatorTools tools = new CalculatorTools();

    @Test
    void addShouldSumTwoNumbers() {
        assertThat(tools.add(2, 3)).isEqualTo(5.0);
        assertThat(tools.add(-1, 1)).isEqualTo(0.0);
        assertThat(tools.add(0, 0)).isEqualTo(0.0);
    }

    @Test
    void subtractShouldComputeDifference() {
        assertThat(tools.subtract(10, 3)).isEqualTo(7.0);
        assertThat(tools.subtract(0, 5)).isEqualTo(-5.0);
    }

    @Test
    void multiplyShouldComputeProduct() {
        assertThat(tools.multiply(4, 5)).isEqualTo(20.0);
        assertThat(tools.multiply(0, 100)).isEqualTo(0.0);
        assertThat(tools.multiply(-2, 3)).isEqualTo(-6.0);
    }

    @Test
    void divideShouldComputeQuotient() {
        assertThat(tools.divide(10, 2)).isEqualTo("5.0");
        assertThat(tools.divide(7, 2)).isEqualTo("3.5");
    }

    @Test
    void divideByZeroShouldReturnErrorString() {
        assertThat(tools.divide(10, 0)).isEqualTo("错误:除数不能为 0");
    }
}
