package io.github.afgprojects.framework.core.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.autoconfigure.VirtualThreadAutoConfiguration;
import io.github.afgprojects.framework.core.autoconfigure.VirtualThreadProperties;
import io.github.afgprojects.framework.core.support.BaseIntegrationTest;

/**
 * VirtualThreadAutoConfiguration 集成测试
 *
 * <p>验证虚拟线程自动配置是否正确注入所有 Bean
 * <p>注意：需要配置 spring.threads.virtual.enabled=true 才能启用虚拟线程
 */
class VirtualThreadAutoConfigurationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("应该成功注入 VirtualThreadProperties Bean")
    void shouldInjectVirtualThreadProperties() {
        // when
        VirtualThreadProperties properties = getBean(VirtualThreadProperties.class);

        // then
        assertThat(properties).isNotNull();
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("应该成功注入 ThreadFactory Bean")
    void shouldInjectThreadFactory() {
        // when
        ThreadFactory threadFactory = getBean(ThreadFactory.class);

        // then
        assertThat(threadFactory).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入虚拟线程执行器 Bean")
    void shouldInjectVirtualThreadExecutor() {
        // when
        Executor executor = (Executor) getBean("afgVirtualThreadExecutor");

        // then
        assertThat(executor).isNotNull();
    }

    @Test
    @DisplayName("应该成功注入 VirtualThreadAutoConfiguration Bean")
    void shouldInjectVirtualThreadAutoConfiguration() {
        // when
        VirtualThreadAutoConfiguration autoConfig = getBean(VirtualThreadAutoConfiguration.class);

        // then
        assertThat(autoConfig).isNotNull();
    }

    @Test
    @DisplayName("所有虚拟线程相关 Bean 应该在 ApplicationContext 中可用")
    void allVirtualThreadBeansShouldBeAvailable() {
        // when & then
        // afg 提供的 Bean
        assertThat(applicationContext.containsBean("virtualThreadFactory")).isTrue();
        assertThat(applicationContext.containsBean("afgVirtualThreadExecutor")).isTrue();
    }

    @Test
    @DisplayName("虚拟线程执行器应该能够执行任务")
    void virtualThreadExecutorShouldExecuteTask() throws Exception {
        // given
        Executor executor = (Executor) getBean("afgVirtualThreadExecutor");
        final String[] result = new String[1];
        final Thread[] executedThread = new Thread[1];

        // when
        executor.execute(() -> {
            executedThread[0] = Thread.currentThread();
            result[0] = Thread.currentThread().getName();
        });

        // 等待执行完成
        Thread.sleep(100);

        // then
        assertThat(result[0]).isNotNull();
        assertThat(executedThread[0]).isNotNull();
        // 验证是虚拟线程（Java 25+）
        assertThat(executedThread[0].isVirtual()).isTrue();
    }

}
