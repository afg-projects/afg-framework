package io.github.afgprojects.framework.security.resource.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logical 枚举测试
 */
@DisplayName("Logical 枚举测试")
class LogicalTest {

    @Nested
    @DisplayName("枚举值")
    class EnumValueTests {

        @Test
        @DisplayName("应包含 AND 和 OR 两个值")
        void shouldContainAndAndOr() {
            Logical[] values = Logical.values();

            assertThat(values).hasSize(2);
            assertThat(values).containsExactlyInAnyOrder(Logical.AND, Logical.OR);
        }

        @Test
        @DisplayName("应能通过名称获取枚举值")
        void shouldGetValueByName() {
            assertThat(Logical.valueOf("AND")).isEqualTo(Logical.AND);
            assertThat(Logical.valueOf("OR")).isEqualTo(Logical.OR);
        }
    }
}
