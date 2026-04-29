package io.github.afgprojects.framework.core.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * MockUtils 测试
 */
class MockUtilsTest extends BaseUnitTest {

    interface TestService {
        String getName();

        int getValue();

        void doSomething();
    }

    @Test
    @DisplayName("创建 Mock 对象")
    void shouldCreateMock() {
        TestService mock = MockUtils.mock(TestService.class);
        assertThat(mock).isNotNull();
        assertThat(MockUtils.isMock(mock)).isTrue();
    }

    @Test
    @DisplayName("创建 Mock 对象并配置")
    void shouldCreateMockWithConfig() {
        TestService mock = MockUtils.mock(TestService.class, (TestService m) -> {
            when(m.getName()).thenReturn("test");
            when(m.getValue()).thenReturn(42);
            return m;
        });

        assertThat(mock.getName()).isEqualTo("test");
        assertThat(mock.getValue()).isEqualTo(42);
    }

    @Test
    @DisplayName("创建松散 Mock 对象")
    void shouldCreateLenientMock() {
        TestService mock = MockUtils.lenientMock(TestService.class);

        // 调用未配置的方法不会抛出异常
        assertThat(mock.getName()).isNull();
        assertThat(mock.getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("创建 Spy 对象")
    void shouldCreateSpy() {
        List<String> realList = new ArrayList<>();
        List<String> spy = MockUtils.spy(realList);

        assertThat(MockUtils.isSpy(spy)).isTrue();

        spy.add("test");
        assertThat(spy).contains("test");
    }

    @Test
    @DisplayName("创建 Spy 对象并配置")
    void shouldCreateSpyWithConfig() {
        List<String> realList = new ArrayList<>();
        realList.add("original");

        List<String> spy = MockUtils.spy(realList, (List<String> s) -> {
            when(s.size()).thenReturn(100);
            return s;
        });

        assertThat(spy.size()).isEqualTo(100);
        assertThat(spy.get(0)).isEqualTo("original");
    }

    @Test
    @DisplayName("创建 Spy 使用类")
    void shouldCreateSpyByClass() {
        ArrayList<String> spy = MockUtils.spy(ArrayList.class);

        assertThat(MockUtils.isSpy(spy)).isTrue();
        assertThat(spy.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("验证 Mock 方法调用次数")
    void shouldVerifyTimes() {
        TestService mock = MockUtils.mock(TestService.class);

        mock.getName();
        mock.getName();
        mock.getValue();

        MockUtils.verify(mock, 2).getName();
        MockUtils.verify(mock, 1).getValue();
    }

    @Test
    @DisplayName("验证 Mock 方法从未被调用")
    void shouldVerifyNever() {
        TestService mock = MockUtils.mock(TestService.class);

        mock.getName();

        MockUtils.verifyNever(mock).getValue();
    }

    @Test
    @DisplayName("验证 Mock 方法被调用至少 n 次")
    void shouldVerifyAtLeast() {
        TestService mock = MockUtils.mock(TestService.class);

        mock.getName();
        mock.getName();
        mock.getName();

        MockUtils.verifyAtLeast(mock, 2).getName();
    }

    @Test
    @DisplayName("验证 Mock 方法被调用最多 n 次")
    void shouldVerifyAtMost() {
        TestService mock = MockUtils.mock(TestService.class);

        mock.getName();
        mock.getName();

        MockUtils.verifyAtMost(mock, 3).getName();
    }

    @Test
    @DisplayName("验证 Mock 方法被调用一次")
    void shouldVerifyOnce() {
        TestService mock = MockUtils.mock(TestService.class);

        mock.getName();

        MockUtils.verifyOnce(mock).getName();
    }

    @Test
    @DisplayName("验证没有更多交互")
    void shouldVerifyNoMoreInteractions() {
        TestService mock = MockUtils.mock(TestService.class);

        mock.getName();
        MockUtils.verifyOnce(mock).getName();
        MockUtils.verifyNoMoreInteractions(mock);
    }

    @Test
    @DisplayName("验证没有交互")
    void shouldVerifyNoInteractions() {
        TestService mock = MockUtils.mock(TestService.class);

        MockUtils.verifyNoInteractions(mock);
    }

    @Test
    @DisplayName("重置 Mock 对象")
    void shouldResetMock() {
        TestService mock = MockUtils.mock(TestService.class);
        when(mock.getName()).thenReturn("test");

        assertThat(mock.getName()).isEqualTo("test");

        MockUtils.reset(mock);

        assertThat(mock.getName()).isNull();
    }

    @Test
    @DisplayName("检查是否是 Mock/Spy")
    void shouldCheckMockOrSpy() {
        TestService mock = MockUtils.mock(TestService.class);
        TestService spy = MockUtils.spy(new TestServiceImpl());

        assertThat(MockUtils.isMock(mock)).isTrue();
        assertThat(MockUtils.isSpy(mock)).isFalse();

        assertThat(MockUtils.isMock(spy)).isTrue();
        assertThat(MockUtils.isSpy(spy)).isTrue();

        assertThat(MockUtils.isMock(new TestServiceImpl())).isFalse();
        assertThat(MockUtils.isSpy(new TestServiceImpl())).isFalse();
    }

    @Test
    @DisplayName("获取调用次数")
    void shouldGetInvocationCount() {
        TestService mock = MockUtils.mock(TestService.class);

        mock.getName();
        mock.getName();
        mock.getValue();

        assertThat(MockUtils.getInvocationCount(mock)).isEqualTo(3);
    }

    @Test
    @DisplayName("清除调用记录")
    void shouldClearInvocations() {
        TestService mock = MockUtils.mock(TestService.class);

        mock.getName();
        assertThat(MockUtils.getInvocationCount(mock)).isEqualTo(1);

        MockUtils.clearInvocations(mock);
        assertThat(MockUtils.getInvocationCount(mock)).isZero();
    }

    @Test
    @DisplayName("创建 Mock Iterator")
    void shouldCreateMockIterator() {
        Iterator<String> iterator = MockUtils.mockIterator("a", "b", "c");

        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("a");
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("b");
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("c");
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    @DisplayName("创建空 Mock Iterator")
    void shouldCreateEmptyMockIterator() {
        Iterator<String> iterator = MockUtils.mockIterator();

        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Mock Map 返回默认值")
    void shouldMockMapWithDefault() {
        @SuppressWarnings("unchecked")
        Map<String, String> map = MockUtils.mock(Map.class);
        MockUtils.mockMapWithDefault(map, "default");

        assertThat(map.get("any-key")).isEqualTo("default");
    }

    @Test
    @DisplayName("配置 Mock 返回值")
    @SuppressWarnings("unchecked")
    void shouldWhenReturn() {
        TestService mock = MockUtils.mock(TestService.class);
        when(mock.getName()).thenReturn("configured");

        assertThat(mock.getName()).isEqualTo("configured");
    }

    @Test
    @DisplayName("配置 Mock 抛出异常")
    void shouldWhenThrow() {
        TestService mock = MockUtils.mock(TestService.class);
        RuntimeException exception = new RuntimeException("test exception");

        when(mock.getName()).thenThrow(exception);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, mock::getName);
    }

    /**
     * TestService 实现类，用于 Spy 测试
     */
    private static class TestServiceImpl implements TestService {
        @Override
        public String getName() {
            return "real-name";
        }

        @Override
        public int getValue() {
            return 100;
        }

        @Override
        public void doSomething() {
            // 空实现
        }
    }
}