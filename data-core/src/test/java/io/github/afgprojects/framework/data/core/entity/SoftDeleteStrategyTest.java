package io.github.afgprojects.framework.data.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SoftDeleteStrategy 测试
 */
@DisplayName("SoftDeleteStrategy 测试")
class SoftDeleteStrategyTest {

    @Nested
    @DisplayName("策略枚举测试")
    class StrategyEnumTests {

        @Test
        @DisplayName("应该有两个策略值")
        void shouldHaveTwoStrategies() {
            // When
            SoftDeleteStrategy[] strategies = SoftDeleteStrategy.values();

            // Then
            assertThat(strategies).hasSize(2);
            assertThat(strategies).contains(SoftDeleteStrategy.BOOLEAN, SoftDeleteStrategy.TIMESTAMP);
        }

        @Test
        @DisplayName("BOOLEAN 策略应该存在")
        void booleanStrategyShouldExist() {
            // When & Then
            assertThat(SoftDeleteStrategy.BOOLEAN).isNotNull();
            assertThat(SoftDeleteStrategy.BOOLEAN.name()).isEqualTo("BOOLEAN");
        }

        @Test
        @DisplayName("TIMESTAMP 策略应该存在")
        void timestampStrategyShouldExist() {
            // When & Then
            assertThat(SoftDeleteStrategy.TIMESTAMP).isNotNull();
            assertThat(SoftDeleteStrategy.TIMESTAMP.name()).isEqualTo("TIMESTAMP");
        }
    }
}
