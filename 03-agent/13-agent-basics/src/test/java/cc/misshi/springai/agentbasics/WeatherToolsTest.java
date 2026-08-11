package cc.misshi.springai.agentbasics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WeatherTools 单元测试 — 0 网络,纯本地.
 *
 * <p>验证 @Tool 方法本身(不依赖 LLM).
 */
class WeatherToolsTest {

    private final WeatherTools tools = new WeatherTools();

    @Test
    void shouldReturnWeatherForKnownCity() {
        var info = tools.getCurrentWeather("北京");

        assertThat(info.city()).isEqualTo("北京");
        assertThat(info.temperature()).isBetween(10, 39);
        assertThat(info.condition()).isIn("晴朗", "多云", "小雨");
        assertThat(info.humidity()).isBetween(40, 79);
    }

    @Test
    void differentCitiesShouldProduceDifferentOrSameResults() {
        // 同城市 hash 一致,结果稳定
        var beijing1 = tools.getCurrentWeather("北京");
        var beijing2 = tools.getCurrentWeather("北京");
        assertThat(beijing1).isEqualTo(beijing2);

        // 不同城市 hash 不同(但可能撞 hash,至少 temp 在合理范围)
        var shanghai = tools.getCurrentWeather("上海");
        assertThat(shanghai.temperature()).isBetween(10, 39);
    }
}
