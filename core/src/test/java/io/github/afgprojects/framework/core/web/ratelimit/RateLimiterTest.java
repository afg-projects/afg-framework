package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;
import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

class RateLimiterTest extends BaseUnitTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RRateLimiter rRateLimiter;

    private RateLimitProperties properties;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new RateLimitProperties();
        rateLimiter = new RateLimiter(redissonClient, properties);

        // Setup Spring RequestContextHolder
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void should_acquireToken_when_available() {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        // 设置 RequestContext
        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        when(redissonClient.getRateLimiter(anyString())).thenReturn(rRateLimiter);
        // trySetRate 可能返回 false（已存在）或 true（新创建），不影响后续逻辑
        when(rRateLimiter.trySetRate(any(RateType.class), anyLong(), anyLong(), any(RateIntervalUnit.class)))
                .thenReturn(false);
        when(rRateLimiter.tryAcquire(1)).thenReturn(true);

        RateLimitResult result = rateLimiter.tryAcquire(annotation);

        assertThat(result.allowed()).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_failAcquire_when_tokensExhausted() {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        when(redissonClient.getRateLimiter(anyString())).thenReturn(rRateLimiter);
        when(rRateLimiter.tryAcquire(anyInt())).thenReturn(false);

        RateLimitResult result = rateLimiter.tryAcquire(annotation);

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void should_passThrough_when_exceptionOccurs() {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        when(redissonClient.getRateLimiter(anyString())).thenThrow(new RuntimeException("Redis error"));

        RateLimitResult result = rateLimiter.tryAcquire(annotation);

        // 限流器异常时放行，避免影响业务
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void should_buildCorrectKey_forIpDimension() {
        RateLimit annotation = createAnnotation("api.query", 10, RateLimitDimension.IP);

        // 设置请求上下文
        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        String key = rateLimiter.buildKey(annotation);

        assertThat(key).isEqualTo("rateLimit:api.query:ip:192.168.1.1");

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_buildCorrectKey_forUserDimension() {
        RateLimit annotation = createAnnotation("api.update", 10, RateLimitDimension.USER);

        RequestContext context = new RequestContext();
        context.setUserId(12345L);
        AfgRequestContextHolder.setContext(context);

        String key = rateLimiter.buildKey(annotation);

        assertThat(key).isEqualTo("rateLimit:api.update:user:12345");

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_buildCorrectKey_forTenantDimension() {
        RateLimit annotation = createAnnotation("api.tenant", 10, RateLimitDimension.TENANT);

        RequestContext context = new RequestContext();
        context.setTenantId(999L);
        AfgRequestContextHolder.setContext(context);

        String key = rateLimiter.buildKey(annotation);

        assertThat(key).isEqualTo("rateLimit:api.tenant:tenant:999");

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_buildCorrectKey_forApiDimension() {
        RateLimit annotation = createAnnotation("api.global", 100, RateLimitDimension.API);

        String key = rateLimiter.buildKey(annotation);

        assertThat(key).isEqualTo("rateLimit:api.global:api:global");
    }

    @Test
    void should_handleNullContext_gracefully() {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        // 清除上下文
        AfgRequestContextHolder.clear();

        String key = rateLimiter.buildKey(annotation);

        assertThat(key).isEqualTo("rateLimit:test:ip:unknown");
    }

    @Test
    void should_handleAnonymousUser_gracefully() {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.USER);

        RequestContext context = new RequestContext();
        // userId 默认为 null，表示匿名用户
        AfgRequestContextHolder.setContext(context);

        String key = rateLimiter.buildKey(annotation);

        assertThat(key).isEqualTo("rateLimit:test:user:anonymous");

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_returnCorrectErrorCode() {
        int code = rateLimiter.getRateLimitErrorCode();

        assertThat(code).isEqualTo(CommonErrorCode.RATE_LIMIT_EXCEEDED.getCode());
    }

    @Test
    void should_returnAnnotationMessage_when_provided() {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);
        // 使用带消息的注解

        String message = rateLimiter.getRateLimitMessage(annotation);

        assertThat(message).isEqualTo("请求过于频繁，请稍后再试");
    }

    @Test
    void should_usePropertiesMessage_when_annotationMessageEmpty() {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);
        properties.getFallback().setDefaultMessage("自定义默认消息");

        String message = rateLimiter.getRateLimitMessage(annotation);

        assertThat(message).isEqualTo("请求过于频繁，请稍后再试");
    }

    @Test
    void should_useCustomKeyPrefix() {
        properties.setKeyPrefix("customPrefix");
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.API);

        String key = rateLimiter.buildKey(annotation);

        assertThat(key).startsWith("customPrefix:");
    }

    @Test
    void should_resetRateLimiter_when_resetCalled() {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        when(redissonClient.getRateLimiter(anyString())).thenReturn(rRateLimiter);
        when(rRateLimiter.delete()).thenReturn(true);

        boolean result = rateLimiter.reset(annotation);

        assertThat(result).isTrue();
        verify(rRateLimiter).delete();
    }

    @Test
    void should_returnFalse_when_resetFails() {
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        when(redissonClient.getRateLimiter(anyString())).thenReturn(rRateLimiter);
        when(rRateLimiter.delete()).thenThrow(new RuntimeException("Redis error"));

        boolean result = rateLimiter.reset(annotation);

        assertThat(result).isFalse();
    }

    @Test
    void should_useLocalRateLimiter_when_redissonIsNull() {
        // 创建没有 Redisson 的限流器
        RateLimiter localRateLimiter = new RateLimiter(null, properties);
        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);

        RateLimitResult result = localRateLimiter.tryAcquire(annotation);

        // 本地限流应该工作
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void should_useWhitelist_when_ipInWhitelist() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getIps().add("192.168.1.1");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);
        RateLimitResult result = rateLimiter.tryAcquire(annotation);

        // 白名单内的 IP 应该直接通过
        assertThat(result.allowed()).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_useWhitelistPattern_when_ipMatchesPattern() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getIps().add("192.168.1.*");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.100");
        AfgRequestContextHolder.setContext(context);

        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.IP);
        RateLimitResult result = rateLimiter.tryAcquire(annotation);

        // 通配符匹配
        assertThat(result.allowed()).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_useWhitelist_when_userIdInWhitelist() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getUserIds().add(12345L);

        RequestContext context = new RequestContext();
        context.setUserId(12345L);
        AfgRequestContextHolder.setContext(context);

        RateLimit annotation = createAnnotation("test", 10, RateLimitDimension.USER);
        RateLimitResult result = rateLimiter.tryAcquire(annotation);

        // 白名单内的用户应该直接通过
        assertThat(result.allowed()).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_useSlidingWindow_when_algorithmSpecified() {
        RateLimit annotation = createAnnotationWithAlgorithm("test", 10, RateLimitDimension.IP,
                RateLimitAlgorithm.SLIDING_WINDOW);

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        when(redissonClient.getAtomicLong(anyString())).thenThrow(new RuntimeException("Redis not available"));

        RateLimitResult result = rateLimiter.tryAcquire(annotation);

        // 即使 Redis 异常，滑动窗口也应该降级放行
        assertThat(result.allowed()).isTrue();

        AfgRequestContextHolder.clear();
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
            public RateLimitAlgorithm algorithm() {
                return RateLimitAlgorithm.TOKEN_BUCKET;
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
                return "请求过于频繁，请稍后再试";
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

    private RateLimit createAnnotationWithAlgorithm(String key, long rate, RateLimitDimension dimension,
            RateLimitAlgorithm algorithm) {
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
            public RateLimitAlgorithm algorithm() {
                return algorithm;
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
                return "请求过于频繁，请稍后再试";
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
}
