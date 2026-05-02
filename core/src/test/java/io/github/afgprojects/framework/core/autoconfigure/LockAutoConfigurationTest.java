package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.lock.DistributedLock;
import io.github.afgprojects.framework.core.lock.LockProperties;

/**
 * LockAutoConfiguration 测试
 */
@DisplayName("LockAutoConfiguration 测试")
class LockAutoConfigurationTest {

    private LockAutoConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new LockAutoConfiguration();
    }

    @Nested
    @DisplayName("lockAspect 配置测试")
    class LockAspectTests {

        @Test
        @DisplayName("应该创建锁切面")
        void shouldCreateLockAspect() {
            DistributedLock distributedLock = mock(DistributedLock.class);
            LockProperties properties = new LockProperties();

            var aspect = configuration.lockAspect(distributedLock, properties);

            assertThat(aspect).isNotNull();
        }
    }
}
