package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitDimension;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitResult;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimiter;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimiterBuilder;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * RateLimitInterceptor 单元测试。
 * <p>
 * 测试限流拦截器的功能，验证限流检查、降级方法和异常处理逻辑。
 *
 * @see RateLimitInterceptor
 */
class RateLimitInterceptorTest extends BaseUnitTest {

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private RateLimiterBuilder rateLimiterBuilder;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private AfgCoreProperties properties;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new AfgCoreProperties();
        interceptor = new RateLimitInterceptor(rateLimiter, properties, null);

        // Setup builder mock chain
        when(rateLimiter.builder()).thenReturn(rateLimiterBuilder);
        when(rateLimiterBuilder.key(any())).thenReturn(rateLimiterBuilder);
        when(rateLimiterBuilder.dimension(any())).thenReturn(rateLimiterBuilder);
        when(rateLimiterBuilder.rate(any(long.class))).thenReturn(rateLimiterBuilder);
        when(rateLimiterBuilder.burst(any(long.class))).thenReturn(rateLimiterBuilder);
        when(rateLimiterBuilder.algorithm(any())).thenReturn(rateLimiterBuilder);
        when(rateLimiterBuilder.windowSize(any(long.class))).thenReturn(rateLimiterBuilder);
    }

    /**
     * 测试当限流禁用时直接执行方法。
     */
    @Test
    void should_proceed_when_rateLimitDisabled() throws Throwable {
        properties.getRateLimit().setEnabled(false);
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        when(joinPoint.proceed()).thenReturn("result");

        Object result = interceptor.around(joinPoint, annotation);

        assertThat(result).isEqualTo("result");
        verify(rateLimiter, never()).builder();
    }

    @Test
    void should_proceed_when_tokenAcquired() throws Throwable {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        when(rateLimiterBuilder.tryAcquire()).thenReturn(RateLimitResult.allowed(10, 20, System.currentTimeMillis() + 1000));
        when(joinPoint.proceed()).thenReturn("result");

        Object result = interceptor.around(joinPoint, annotation);

        assertThat(result).isEqualTo("result");
        verify(rateLimiter).builder();
    }

    @Test
    void should_throwException_when_rateLimitExceeded() throws Throwable {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        when(rateLimiterBuilder.tryAcquire()).thenReturn(RateLimitResult.rejected(20, System.currentTimeMillis() + 1000, 100));

        try {
            interceptor.around(joinPoint, annotation);
        } catch (BusinessException e) {
            assertThat(e.getCode()).isEqualTo(CommonErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        }
    }

    @Test
    void should_executeFallback_when_fallbackMethodProvided() throws Throwable {
        RateLimit annotation = createAnnotationWithFallback("test", 10, RateLimitDimension.IP, "fallback");

        when(rateLimiterBuilder.tryAcquire()).thenReturn(RateLimitResult.rejected(20, System.currentTimeMillis() + 1000, 100));
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[] {"arg"});
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(TestService.class.getMethod("execute", String.class));

        Object result = interceptor.around(joinPoint, annotation);

        assertThat(result).isEqualTo("fallback-result");
    }

    @Test
    void should_throwException_when_fallbackMethodNotFound() throws Throwable {
        RateLimit annotation = createAnnotationWithFallback("test", 10, RateLimitDimension.IP, "nonExistFallback");

        when(rateLimiterBuilder.tryAcquire()).thenReturn(RateLimitResult.rejected(20, System.currentTimeMillis() + 1000, 100));
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[] {"arg"});
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(TestService.class.getMethod("execute", String.class));

        try {
            interceptor.around(joinPoint, annotation);
        } catch (BusinessException e) {
            assertThat(e.getCode()).isEqualTo(CommonErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        }
    }

    @Test
    void should_throwException_when_fallbackDisabled() throws Throwable {
        properties.getRateLimit().getFallback().setEnabled(false);
        RateLimit annotation = createAnnotationWithFallback("test", 10, RateLimitDimension.IP, "fallback");

        when(rateLimiterBuilder.tryAcquire()).thenReturn(RateLimitResult.rejected(20, System.currentTimeMillis() + 1000, 100));

        try {
            interceptor.around(joinPoint, annotation);
        } catch (BusinessException e) {
            assertThat(e.getCode()).isEqualTo(CommonErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        }
    }

    private RateLimit createAnnotation(String key, long rate, RateLimitDimension dimension) {
        return new RateLimit() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public long rate() {
                return rate;
            }

            @Override
            public long burst() {
                return 0;
            }

            @Override
            public RateLimitDimension dimension() {
                return dimension;
            }

            @Override
            public AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm algorithm() {
                return AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.TOKEN_BUCKET;
            }

            @Override
            public long windowSize() {
                return 1;
            }

            @Override
            public String fallbackMethod() {
                return "";
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public boolean responseHeaders() {
                return true;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }
        };
    }

    private RateLimit createAnnotationWithFallback(String key, long rate, RateLimitDimension dimension, String fallbackMethod) {
        return new RateLimit() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public long rate() {
                return rate;
            }

            @Override
            public long burst() {
                return 0;
            }

            @Override
            public RateLimitDimension dimension() {
                return dimension;
            }

            @Override
            public AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm algorithm() {
                return AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.TOKEN_BUCKET;
            }

            @Override
            public long windowSize() {
                return 1;
            }

            @Override
            public String fallbackMethod() {
                return fallbackMethod;
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public boolean responseHeaders() {
                return true;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RateLimit.class;
            }
        };
    }

    // 测试服务类
    static class TestService {

        @RateLimit(key = "test")
        public String execute(String arg) {
            return "result";
        }

        public String fallback(String arg) {
            return "fallback-result";
        }
    }
}
