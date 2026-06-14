package io.github.afgprojects.framework.data.jdbc.datasource;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DataSourceContextHolder 单元测试
 * <p>
 * 测试 ThreadLocal 栈结构的数据源上下文管理：push/pop/peek/clear 语义。
 * 纯逻辑测试，不需要 Spring 上下文。
 * </p>
 */
class DataSourceContextHolderTest {

    @AfterEach
    void tearDown() {
        DataSourceContextHolder.clear();
    }

    @Nested
    @DisplayName("push/pop 栈操作")
    class PushPop {

        @Test
        @DisplayName("should return specified data source when push")
        void shouldReturnSpecifiedDataSource_whenPush() {
            DataSourceContextHolder.push("slave_1");

            assertThat(DataSourceContextHolder.get()).isEqualTo("slave_1");
            assertThat(DataSourceContextHolder.isSpecified()).isTrue();
        }

        @Test
        @DisplayName("should restore previous data source when pop")
        void shouldRestorePreviousDataSource_whenPop() {
            DataSourceContextHolder.push("slave_1");
            DataSourceContextHolder.push("slave_2");

            assertThat(DataSourceContextHolder.get()).isEqualTo("slave_2");

            DataSourceContextHolder.pop();

            assertThat(DataSourceContextHolder.get()).isEqualTo("slave_1");
        }

        @Test
        @DisplayName("should return null when pop all entries")
        void shouldReturnNull_whenPopAllEntries() {
            DataSourceContextHolder.push("slave_1");
            DataSourceContextHolder.pop();

            assertThat(DataSourceContextHolder.get()).isNull();
            assertThat(DataSourceContextHolder.isSpecified()).isFalse();
        }

        @Test
        @DisplayName("should handle nested push/pop correctly")
        void shouldHandleNestedPushPopCorrectly() {
            DataSourceContextHolder.push("master");
            assertThat(DataSourceContextHolder.get()).isEqualTo("master");

            DataSourceContextHolder.push("slave_1");
            assertThat(DataSourceContextHolder.get()).isEqualTo("slave_1");

            DataSourceContextHolder.push("slave_2");
            assertThat(DataSourceContextHolder.get()).isEqualTo("slave_2");

            DataSourceContextHolder.pop();
            assertThat(DataSourceContextHolder.get()).isEqualTo("slave_1");

            DataSourceContextHolder.pop();
            assertThat(DataSourceContextHolder.get()).isEqualTo("master");

            DataSourceContextHolder.pop();
            assertThat(DataSourceContextHolder.get()).isNull();
        }

        @Test
        @DisplayName("should be no-op when pop on empty stack")
        void shouldBeNoOp_whenPopOnEmptyStack() {
            assertThatCode(() -> DataSourceContextHolder.pop()).doesNotThrowAnyException();
            assertThat(DataSourceContextHolder.get()).isNull();
        }
    }

    @Nested
    @DisplayName("set 替换操作")
    class SetOperation {

        @Test
        @DisplayName("should replace current data source when set")
        void shouldReplaceCurrentDataSource_whenSet() {
            DataSourceContextHolder.push("slave_1");
            DataSourceContextHolder.set("slave_2");

            // set 替换栈顶
            assertThat(DataSourceContextHolder.get()).isEqualTo("slave_2");
        }

        @Test
        @DisplayName("should clear data source when set null")
        void shouldClearDataSource_whenSetNull() {
            DataSourceContextHolder.push("slave_1");
            DataSourceContextHolder.set(null);

            assertThat(DataSourceContextHolder.get()).isNull();
        }
    }

    @Nested
    @DisplayName("clear 清除操作")
    class ClearOperation {

        @Test
        @DisplayName("should clear all data sources when clear")
        void shouldClearAllDataSources_whenClear() {
            DataSourceContextHolder.push("slave_1");
            DataSourceContextHolder.push("slave_2");

            DataSourceContextHolder.clear();

            assertThat(DataSourceContextHolder.get()).isNull();
            assertThat(DataSourceContextHolder.isSpecified()).isFalse();
        }
    }

    @Nested
    @DisplayName("线程隔离")
    class ThreadIsolation {

        @Test
        @DisplayName("should not leak data source between threads")
        void shouldNotLeakDataSourceBetweenThreads() throws Exception {
            DataSourceContextHolder.push("main-ds");

            Thread thread = new Thread(() -> {
                assertThat(DataSourceContextHolder.get()).isNull();
                DataSourceContextHolder.push("thread-ds");
                assertThat(DataSourceContextHolder.get()).isEqualTo("thread-ds");
            });
            thread.start();
            thread.join(5000);

            // 主线程的数据源不受其他线程影响
            assertThat(DataSourceContextHolder.get()).isEqualTo("main-ds");
        }
    }

    @Nested
    @DisplayName("peek 查看操作")
    class PeekOperation {

        @Test
        @DisplayName("should return top without removing when peek")
        void shouldReturnTopWithoutRemoving_whenPeek() {
            DataSourceContextHolder.push("slave_1");

            assertThat(DataSourceContextHolder.peek()).isEqualTo("slave_1");
            assertThat(DataSourceContextHolder.peek()).isEqualTo("slave_1"); // 再次查看仍然有值
        }

        @Test
        @DisplayName("should return null when peek on empty stack")
        void shouldReturnNull_whenPeekOnEmptyStack() {
            assertThat(DataSourceContextHolder.peek()).isNull();
        }
    }

    @Nested
    @DisplayName("isSpecified 判断")
    class IsSpecified {

        @Test
        @DisplayName("should return false when no data source specified")
        void shouldReturnFalse_whenNoDataSourceSpecified() {
            assertThat(DataSourceContextHolder.isSpecified()).isFalse();
        }

        @Test
        @DisplayName("should return true when data source is specified")
        void shouldReturnTrue_whenDataSourceIsSpecified() {
            DataSourceContextHolder.push("master");
            assertThat(DataSourceContextHolder.isSpecified()).isTrue();
        }
    }
}
