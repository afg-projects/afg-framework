package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.web.trace.TraceContext;
import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * BaggageContext 测试类
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaggageContext 测试")
class BaggageContextTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Baggage baggage;

    // A mock that implements both Tracer and BaggageManager
    private Tracer tracerWithBaggage;

    @BeforeEach
    void setUp() {
        BaggageContext.clear();
        TraceContext.setTracer(null);
        // 创建同时实现 Tracer 和 BaggageManager 的 mock
        tracerWithBaggage = mock(Tracer.class, Mockito.withSettings().extraInterfaces(BaggageManager.class));
    }

    @Test
    @DisplayName("设置和获取 Baggage 值")
    void testSetAndGet() {
        BaggageContext.set("customField", "value123");

        assertThat(BaggageContext.get("customField")).isEqualTo("value123");
    }

    @Test
    @DisplayName("设置 null 值会移除字段")
    void testSetNullRemovesField() {
        BaggageContext.set("field", "value");
        BaggageContext.set("field", null);

        assertThat(BaggageContext.get("field")).isNull();
    }

    @Test
    @DisplayName("获取不存在的字段返回 null")
    void testGetNonExistentField() {
        assertThat(BaggageContext.get("nonexistent")).isNull();
    }

    @Test
    @DisplayName("获取所有 Baggage")
    void testGetAll() {
        BaggageContext.set("field1", "value1");
        BaggageContext.set("field2", "value2");

        Map<String, String> all = BaggageContext.getAll();

        assertThat(all).containsEntry("field1", "value1");
        assertThat(all).containsEntry("field2", "value2");
    }

    @Test
    @DisplayName("清除所有 Baggage")
    void testClear() {
        BaggageContext.set("field1", "value1");
        BaggageContext.set("field2", "value2");

        BaggageContext.clear();

        assertThat(BaggageContext.getAll()).isEmpty();
    }

    @Test
    @DisplayName("判断是否包含字段")
    void testContains() {
        BaggageContext.set("field", "value");

        assertThat(BaggageContext.contains("field")).isTrue();
        assertThat(BaggageContext.contains("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("移除指定字段")
    void testRemove() {
        BaggageContext.set("field1", "value1");
        BaggageContext.set("field2", "value2");

        BaggageContext.remove("field1");

        assertThat(BaggageContext.get("field1")).isNull();
        assertThat(BaggageContext.get("field2")).isEqualTo("value2");
    }

    @Test
    @DisplayName("设置租户ID")
    void testSetTenantId() {
        BaggageContext.setTenantId("tenant123");

        assertThat(BaggageContext.getTenantId()).isEqualTo("tenant123");
    }

    @Test
    @DisplayName("设置用户ID")
    void testSetUserId() {
        BaggageContext.setUserId("user123");

        assertThat(BaggageContext.getUserId()).isEqualTo("user123");
    }

    @Test
    @DisplayName("使用 BaggageManager 设置和获取")
    void testWithBaggageManager() {
        // BaggageManager extends Tracer, so we cast the mock
        BaggageManager baggageManagerMock = (BaggageManager) tracerWithBaggage;
        when(baggageManagerMock.getBaggage("testField")).thenReturn(baggage);
        when(baggage.get()).thenReturn("baggageValue");
        TraceContext.setTracer(tracerWithBaggage);

        assertThat(BaggageContext.get("testField")).isEqualTo("baggageValue");

        verify(baggageManagerMock).getBaggage("testField");
    }
}