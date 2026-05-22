package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.lock.DistributedLock;

/**
 * LockAutoConfiguration 单元测试。
 * 测试分布式锁自动配置类的 Bean 创建功能。
 *
 * @see LockAutoConfiguration
 */
@DisplayName("LockAutoConfiguration 测试")
class LockAutoConfigurationTest {

    private LockAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new LockAutoConfiguration();
    }

    /**
     * 锁切面配置测试。
     * 验证 lockAspect Bean 的创建。
     */
    @Nested
    @DisplayName("lockAspect 配置测试")
    class LockAspectTests {

        /**
         * 测试创建锁切面。
         */
        @Test
        @DisplayName("应该创建锁切面")
        void shouldCreateLockAspect() {
            DistributedLock distributedLock = mock(DistributedLock.class);
            AfgCoreProperties properties = new AfgCoreProperties();

            var aspect = configuration.lockAspect(distributedLock, properties);

            assertThat(aspect).isNotNull();
        }
    }
}
