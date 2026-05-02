package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * VirtualThreadAutoConfiguration 测试
 */
@DisplayName("VirtualThreadAutoConfiguration 测试")
class VirtualThreadAutoConfigurationTest {

    private VirtualThreadAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new VirtualThreadAutoConfiguration();
    }

    @Nested
    @DisplayName("virtualThreadFactory 配置测试")
    class VirtualThreadFactoryTests {

        @Test
        @DisplayName("应该创建虚拟线程工厂")
        void shouldCreateVirtualThreadFactory() {
            VirtualThreadProperties properties = new VirtualThreadProperties();
            properties.setNamePrefix("test-vt-");

            ThreadFactory factory = configuration.virtualThreadFactory(properties);

            assertThat(factory).isNotNull();

            Thread thread = factory.newThread(() -> {});
            assertThat(thread).isNotNull();
            assertThat(thread.isVirtual()).isTrue();
        }
    }

    @Nested
    @DisplayName("afgVirtualThreadExecutor 配置测试")
    class AfgVirtualThreadExecutorTests {

        @Test
        @DisplayName("应该创建虚拟线程执行器")
        void shouldCreateVirtualThreadExecutor() {
            Executor executor = configuration.afgVirtualThreadExecutor();

            assertThat(executor).isNotNull();
        }

        @Test
        @DisplayName("执行器应该创建虚拟线程")
        void executorShouldCreateVirtualThreads() {
            Executor executor = configuration.afgVirtualThreadExecutor();
            Thread[] capturedThread = new Thread[1];

            executor.execute(() -> {
                capturedThread[0] = Thread.currentThread();
            });

            // 等待线程执行
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertThat(capturedThread[0]).isNotNull();
            assertThat(capturedThread[0].isVirtual()).isTrue();
        }
    }

    @Nested
    @DisplayName("VirtualThreadAsyncTaskExecutor 测试")
    class VirtualThreadAsyncTaskExecutorTests {

        @Test
        @DisplayName("应该创建异步任务执行器")
        void shouldCreateAsyncTaskExecutor() {
            VirtualThreadProperties properties = new VirtualThreadProperties();
            ThreadFactory factory = configuration.virtualThreadFactory(properties);

            var asyncExecutor = new VirtualThreadAutoConfiguration.VirtualThreadAsyncTaskExecutor(factory);

            assertThat(asyncExecutor).isNotNull();
        }
    }
}
