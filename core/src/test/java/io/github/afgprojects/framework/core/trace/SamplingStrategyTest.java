package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SamplingStrategy 测试
 */
@DisplayName("SamplingStrategy 测试")
class SamplingStrategyTest {

    @Test
    @DisplayName("应该包含所有采样策略")
    void shouldContainAllStrategies() {
        SamplingStrategy[] strategies = SamplingStrategy.values();

        assertThat(strategies).hasSize(4);
        assertThat(strategies).contains(
                SamplingStrategy.PROBABILITY,
                SamplingStrategy.RATE_LIMITING,
                SamplingStrategy.ALWAYS,
                SamplingStrategy.NEVER
        );
    }

    @Test
    @DisplayName("应该正确获取枚举名称")
    void shouldGetName() {
        assertThat(SamplingStrategy.PROBABILITY.name()).isEqualTo("PROBABILITY");
        assertThat(SamplingStrategy.RATE_LIMITING.name()).isEqualTo("RATE_LIMITING");
        assertThat(SamplingStrategy.ALWAYS.name()).isEqualTo("ALWAYS");
        assertThat(SamplingStrategy.NEVER.name()).isEqualTo("NEVER");
    }
}
