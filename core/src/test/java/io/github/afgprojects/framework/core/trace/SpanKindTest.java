package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SpanKind 测试
 */
@DisplayName("SpanKind 测试")
class SpanKindTest {

    @Test
    @DisplayName("应该包含所有 Span 类型")
    void shouldContainAllKinds() {
        SpanKind[] kinds = SpanKind.values();

        assertThat(kinds).hasSize(5);
        assertThat(kinds).contains(
                SpanKind.SERVER,
                SpanKind.CLIENT,
                SpanKind.PRODUCER,
                SpanKind.CONSUMER,
                SpanKind.INTERNAL
        );
    }

    @Test
    @DisplayName("应该正确获取枚举名称")
    void shouldGetName() {
        assertThat(SpanKind.SERVER.name()).isEqualTo("SERVER");
        assertThat(SpanKind.CLIENT.name()).isEqualTo("CLIENT");
        assertThat(SpanKind.PRODUCER.name()).isEqualTo("PRODUCER");
        assertThat(SpanKind.CONSUMER.name()).isEqualTo("CONSUMER");
        assertThat(SpanKind.INTERNAL.name()).isEqualTo("INTERNAL");
    }
}
