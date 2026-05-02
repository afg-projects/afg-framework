package io.github.afgprojects.framework.core.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.support.TestApplication;

/**
 * LockAspect 集成测试
 */
@DisplayName("LockAspect 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.lock.enabled=true",
                "afg.lock.key-prefix=afg:lock:",
                "afg.lock.default-wait-time=5000",
                "afg.lock.default-lease-time=30000",
                "afg.lock.annotations.enabled=true"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LockAspectIntegrationTest {

    @Autowired(required = false)
    private LockProperties lockProperties;

    @Nested
    @DisplayName("锁配置测试")
    class LockConfigTests {

        @Test
        @DisplayName("应该自动配置锁属性")
        void shouldAutoConfigureLockProperties() {
            assertThat(lockProperties).isNotNull();
            assertThat(lockProperties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("应该正确配置键前缀")
        void shouldConfigureKeyPrefix() {
            assertThat(lockProperties.getKeyPrefix()).isEqualTo("afg:lock:");
        }

        @Test
        @DisplayName("应该正确配置默认等待时间")
        void shouldConfigureDefaultWaitTime() {
            assertThat(lockProperties.getDefaultWaitTime()).isEqualTo(5000);
        }

        @Test
        @DisplayName("应该正确配置默认租约时间")
        void shouldConfigureDefaultLeaseTime() {
            assertThat(lockProperties.getDefaultLeaseTime()).isEqualTo(30000);
        }

        @Test
        @DisplayName("应该正确配置注解启用状态")
        void shouldConfigureAnnotationsEnabled() {
            assertThat(lockProperties.getAnnotations().isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Lock 注解测试")
    class LockAnnotationTests {

        @Test
        @DisplayName("Lock 注解应该有正确的默认值")
        void lockAnnotationShouldHaveCorrectDefaults() throws NoSuchMethodException {
            // Lock 注解的默认值测试
            Lock lock = TestLockClass.class.getMethod("testMethod").getAnnotation(Lock.class);

            assertThat(lock).isNotNull();
            assertThat(lock.key()).isEqualTo("test-key");
            assertThat(lock.waitTime()).isEqualTo(-1);
            assertThat(lock.leaseTime()).isEqualTo(-1);
            assertThat(lock.timeUnit()).isEqualTo(Lock.TimeUnit.MILLISECONDS);
        }
    }

    @Nested
    @DisplayName("LockType 枚举测试")
    class LockTypeTests {

        @Test
        @DisplayName("应该包含所有锁类型")
        void shouldContainAllLockTypes() {
            LockType[] types = LockType.values();

            assertThat(types).contains(
                    LockType.REENTRANT,
                    LockType.FAIR,
                    LockType.READ,
                    LockType.WRITE
            );
        }
    }

    // Test class for annotation testing
    static class TestLockClass {
        @Lock(key = "test-key")
        public void testMethod() {
        }
    }
}
