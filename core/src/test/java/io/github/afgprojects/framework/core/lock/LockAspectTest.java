package io.github.afgprojects.framework.core.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.afgprojects.framework.core.lock.exception.LockAcquisitionException;

/**
 * LockAspect 测试
 */
@DisplayName("LockAspect 测试")
class LockAspectTest {

    @Mock
    private DistributedLock distributedLock;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private LockProperties properties;
    private LockAspect lockAspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new LockProperties();
        lockAspect = new LockAspect(distributedLock, properties);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getDeclaringType()).thenReturn((Class) TestService.class);
        when(methodSignature.getMethod()).thenReturn(getTestMethod());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
    }

    @Nested
    @DisplayName("aroundLock 测试")
    class AroundLockTests {

        @Test
        @DisplayName("成功获取锁应该执行方法并释放锁")
        void shouldExecuteMethodAndReleaseLock() throws Throwable {
            // given
            Lock annotation = createLockAnnotation("test-key", -1, -1, LockType.REENTRANT, true);
            when(distributedLock.tryLock(any(), anyLong(), anyLong(), any())).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = lockAspect.aroundLock(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(distributedLock).tryLock(any(), anyLong(), anyLong(), any());
            verify(joinPoint).proceed();
            verify(distributedLock).unlock(any(), any());
        }

        @Test
        @DisplayName("获取锁失败且 throwOnFailure=true 应该抛出异常")
        void shouldThrowExceptionWhenLockFailed() throws Throwable {
            // given
            Lock annotation = createLockAnnotation("test-key", -1, -1, LockType.REENTRANT, true);
            when(distributedLock.tryLock(any(), anyLong(), anyLong(), any())).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> lockAspect.aroundLock(joinPoint, annotation))
                    .isInstanceOf(LockAcquisitionException.class)
                    .hasMessageContaining("获取锁失败");

            verify(joinPoint, never()).proceed();
        }

        @Test
        @DisplayName("获取锁失败且 throwOnFailure=false 应该继续执行方法")
        void shouldProceedWhenLockFailedAndNotThrow() throws Throwable {
            // given
            Lock annotation = createLockAnnotation("test-key", -1, -1, LockType.REENTRANT, false);
            when(distributedLock.tryLock(any(), anyLong(), anyLong(), any())).thenReturn(false);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            Object result = lockAspect.aroundLock(joinPoint, annotation);

            // then
            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
            verify(distributedLock, never()).unlock(any(), any());
        }

        @Test
        @DisplayName("方法执行异常时应该释放锁")
        void shouldReleaseLockOnException() throws Throwable {
            // given
            Lock annotation = createLockAnnotation("test-key", -1, -1, LockType.REENTRANT, true);
            when(distributedLock.tryLock(any(), anyLong(), anyLong(), any())).thenReturn(true);
            when(joinPoint.proceed()).thenThrow(new RuntimeException("Business error"));

            // when & then
            assertThatThrownBy(() -> lockAspect.aroundLock(joinPoint, annotation))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Business error");

            verify(distributedLock).unlock(any(), any());
        }

        @Test
        @DisplayName("应该使用注解配置的等待时间和持有时间")
        void shouldUseAnnotationConfiguredTimes() throws Throwable {
            // given
            Lock annotation = createLockAnnotation("test-key", 3000, 60000, LockType.REENTRANT, true);
            when(distributedLock.tryLock(any(), eq(3000L), eq(60000L), any())).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            lockAspect.aroundLock(joinPoint, annotation);

            // then
            verify(distributedLock).tryLock(any(), eq(3000L), eq(60000L), any());
        }

        @Test
        @DisplayName("应该使用全局配置的默认时间")
        void shouldUseGlobalDefaultTimes() throws Throwable {
            // given
            properties.setDefaultWaitTime(10000);
            properties.setDefaultLeaseTime(120000);
            Lock annotation = createLockAnnotation("test-key", -1, -1, LockType.REENTRANT, true);
            when(distributedLock.tryLock(any(), eq(10000L), eq(120000L), any())).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            lockAspect.aroundLock(joinPoint, annotation);

            // then
            verify(distributedLock).tryLock(any(), eq(10000L), eq(120000L), any());
        }

        @Test
        @DisplayName("应该使用指定的锁类型")
        void shouldUseSpecifiedLockType() throws Throwable {
            // given
            Lock annotation = createLockAnnotation("test-key", -1, -1, LockType.FAIR, true);
            when(distributedLock.tryLock(any(), anyLong(), anyLong(), eq(LockType.FAIR))).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            lockAspect.aroundLock(joinPoint, annotation);

            // then
            verify(distributedLock).tryLock(any(), anyLong(), anyLong(), eq(LockType.FAIR));
            verify(distributedLock).unlock(any(), eq(LockType.FAIR));
        }
    }

    @Nested
    @DisplayName("SpEL 表达式测试")
    class SpelExpressionTests {

        @Test
        @DisplayName("应该正确解析 SpEL 表达式")
        void shouldResolveSpelExpression() throws Throwable {
            // given
            Lock annotation = createLockAnnotation("#userId", -1, -1, LockType.REENTRANT, true);
            setupMethodParameters("user123");
            when(distributedLock.tryLock(eq("afg:lock:user123"), anyLong(), anyLong(), any())).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            lockAspect.aroundLock(joinPoint, annotation);

            // then
            verify(distributedLock).tryLock(eq("afg:lock:user123"), anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("应该正确处理键前缀")
        void shouldHandleKeyPrefix() throws Throwable {
            // given
            Lock annotation = createLockAnnotation("#userId", "custom", -1, -1, LockType.REENTRANT, true);
            setupMethodParameters("user123");
            when(distributedLock.tryLock(eq("custom:user123"), anyLong(), anyLong(), any())).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            lockAspect.aroundLock(joinPoint, annotation);

            // then
            verify(distributedLock).tryLock(eq("custom:user123"), anyLong(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("时间单位转换测试")
    class TimeUnitConversionTests {

        @Test
        @DisplayName("应该正确转换秒到毫秒")
        void shouldConvertSecondsToMillis() throws Throwable {
            // given
            Lock annotation = createLockAnnotationWithTimeUnit("test-key", 5, 1, Lock.TimeUnit.SECONDS);
            when(distributedLock.tryLock(any(), eq(5000L), eq(1000L), any())).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            lockAspect.aroundLock(joinPoint, annotation);

            // then
            verify(distributedLock).tryLock(any(), eq(5000L), eq(1000L), any());
        }

        @Test
        @DisplayName("应该正确转换分钟到毫秒")
        void shouldConvertMinutesToMillis() throws Throwable {
            // given
            Lock annotation = createLockAnnotationWithTimeUnit("test-key", 1, 2, Lock.TimeUnit.MINUTES);
            when(distributedLock.tryLock(any(), eq(60000L), eq(120000L), any())).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");

            // when
            lockAspect.aroundLock(joinPoint, annotation);

            // then
            verify(distributedLock).tryLock(any(), eq(60000L), eq(120000L), any());
        }
    }

    /**
     * 创建锁注解实例
     */
    private Lock createLockAnnotation(String key, long waitTime, long leaseTime, LockType lockType, boolean throwOnFailure) {
        return createLockAnnotation(key, "", waitTime, leaseTime, lockType, throwOnFailure);
    }

    /**
     * 创建锁注解实例（带前缀）
     */
    private Lock createLockAnnotation(String key, String prefix, long waitTime, long leaseTime, LockType lockType, boolean throwOnFailure) {
        return new Lock() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public String prefix() {
                return prefix;
            }

            @Override
            public long waitTime() {
                return waitTime;
            }

            @Override
            public long leaseTime() {
                return leaseTime;
            }

            @Override
            public TimeUnit timeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            public LockType lockType() {
                return lockType;
            }

            @Override
            public String message() {
                return "获取锁失败";
            }

            @Override
            public boolean throwOnFailure() {
                return throwOnFailure;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Lock.class;
            }
        };
    }

    /**
     * 创建带时间单位的锁注解
     */
    private Lock createLockAnnotationWithTimeUnit(String key, long waitTime, long leaseTime, Lock.TimeUnit timeUnit) {
        return new Lock() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public String prefix() {
                return "";
            }

            @Override
            public long waitTime() {
                return waitTime;
            }

            @Override
            public long leaseTime() {
                return leaseTime;
            }

            @Override
            public TimeUnit timeUnit() {
                return timeUnit;
            }

            @Override
            public LockType lockType() {
                return LockType.REENTRANT;
            }

            @Override
            public String message() {
                return "获取锁失败";
            }

            @Override
            public boolean throwOnFailure() {
                return true;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Lock.class;
            }
        };
    }

    /**
     * 设置方法参数
     */
    private void setupMethodParameters(Object... args) {
        when(joinPoint.getArgs()).thenReturn(args);
        // methodSignature.getMethod() 已经在 setUp 中设置了
        // 它会返回 TestService.testMethod(String userId)
        // 所以 SpEL 表达式 #userId 应该能正确解析
    }

    /**
     * 获取测试方法
     */
    private Method getTestMethod() {
        try {
            return TestService.class.getMethod("testMethod", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 测试服务类
     */
    public static class TestService {
        public String testMethod(String userId) {
            return "result";
        }
    }
}
