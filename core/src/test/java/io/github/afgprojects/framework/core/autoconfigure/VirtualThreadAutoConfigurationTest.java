package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * VirtualThreadAutoConfiguration 单元测试。
 * 测试虚拟线程自动配置类的 Bean 创建功能。
 *
 * @see VirtualThreadAutoConfiguration
 */
@DisplayName("VirtualThreadAutoConfiguration 测试")
class VirtualThreadAutoConfigurationTest {

    private VirtualThreadAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new VirtualThreadAutoConfiguration();
    }

    /**
     * 虚拟线程工厂配置测试。
     * 验证 virtualThreadFactory Bean 的创建和功能。
     */
    @Nested
    @DisplayName("virtualThreadFactory 配置测试")
    class VirtualThreadFactoryTests {

        /**
         * 测试创建虚拟线程工厂。
         * 验证工厂能够创建虚拟线程。
         */
        @Test
        @DisplayName("应该创建虚拟线程工厂")
        void shouldCreateVirtualThreadFactory() {
            AfgCoreProperties properties = new AfgCoreProperties();
            properties.getVirtualThread().setNamePrefix("test-vt-");

            ThreadFactory factory = configuration.virtualThreadFactory(properties);

            assertThat(factory).isNotNull();

            Thread thread = factory.newThread(() -> {});
            assertThat(thread).isNotNull();
            assertThat(thread.isVirtual()).isTrue();
        }
    }

    /**
     * 虚拟线程执行器配置测试。
     * 验证 afgVirtualThreadExecutor Bean 的创建和功能。
     */
    @Nested
    @DisplayName("afgVirtualThreadExecutor 配置测试")
    class AfgVirtualThreadExecutorTests {

        /**
         * 测试创建虚拟线程执行器。
         */
        @Test
        @DisplayName("应该创建虚拟线程执行器")
        void shouldCreateVirtualThreadExecutor() {
            Executor executor = configuration.afgVirtualThreadExecutor();

            assertThat(executor).isNotNull();
        }

        /**
         * 测试执行器创建虚拟线程。
         * 验证通过执行器执行的任务在虚拟线程中运行。
         */
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

    /**
     * 异步任务执行器测试。
     * 验证 VirtualThreadAsyncTaskExecutor 的创建。
     */
    @Nested
    @DisplayName("VirtualThreadAsyncTaskExecutor 测试")
    class VirtualThreadAsyncTaskExecutorTests {

        /**
         * 测试创建异步任务执行器。
         */
        @Test
        @DisplayName("应该创建异步任务执行器")
        void shouldCreateAsyncTaskExecutor() {
            AfgCoreProperties properties = new AfgCoreProperties();
            ThreadFactory factory = configuration.virtualThreadFactory(properties);

            var asyncExecutor = new VirtualThreadAutoConfiguration.VirtualThreadAsyncTaskExecutor(factory);

            assertThat(asyncExecutor).isNotNull();
        }
    }
}
