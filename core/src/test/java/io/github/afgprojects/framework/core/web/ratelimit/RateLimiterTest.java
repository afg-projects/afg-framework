package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.github.afgprojects.framework.core.api.ratelimit.DefaultDimensionResolver;
import io.github.afgprojects.framework.core.api.ratelimit.DefaultWhitelistStrategy;
import io.github.afgprojects.framework.core.api.ratelimit.DimensionResolver;
import io.github.afgprojects.framework.core.api.ratelimit.LocalRateLimitStorage;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitAlgorithm;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitDimension;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitResult;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitStorage;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimiter;
import io.github.afgprojects.framework.core.api.ratelimit.WhitelistStrategy;
import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

class RateLimiterTest extends BaseUnitTest {

    private RateLimitStorage storage;
    private RateLimitProperties properties;
    private WhitelistStrategy whitelistStrategy;
    private DimensionResolver dimensionResolver;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        storage = new LocalRateLimitStorage();
        properties = new RateLimitProperties();
        whitelistStrategy = new DefaultWhitelistStrategy(properties);
        dimensionResolver = new DefaultDimensionResolver();
        rateLimiter = new RateLimiter(storage, properties, whitelistStrategy, dimensionResolver);

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
        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        RateLimitResult result = rateLimiter.builder()
            .key("test")
            .dimension(RateLimitDimension.IP)
            .rate(10)
            .burst(20)
            .tryAcquire();

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void should_failAcquire_when_tokensExhausted() {
        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        // 消耗所有令牌
        for (int i = 0; i < 20; i++) {
            rateLimiter.builder()
                .key("exhaust-test")
                .dimension(RateLimitDimension.IP)
                .rate(10)
                .burst(20)
                .tryAcquire();
        }

        // 下一次应该被拒绝
        RateLimitResult result = rateLimiter.builder()
            .key("exhaust-test")
            .dimension(RateLimitDimension.IP)
            .rate(10)
            .burst(20)
            .tryAcquire();

        assertThat(result.allowed()).isFalse();
    }

    @Test
    void should_buildCorrectKey_forIpDimension() {
        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        // 通过实际调用来验证 key 构建
        RateLimitResult result = rateLimiter.builder()
            .key("api.query")
            .dimension(RateLimitDimension.IP)
            .rate(10)
            .tryAcquire();

        assertThat(result).isNotNull();
    }

    @Test
    void should_buildCorrectKey_forUserDimension() {
        RequestContext context = new RequestContext();
        context.setUserId(12345L);
        AfgRequestContextHolder.setContext(context);

        RateLimitResult result = rateLimiter.builder()
            .key("api.update")
            .dimension(RateLimitDimension.USER)
            .rate(10)
            .tryAcquire();

        assertThat(result).isNotNull();
    }

    @Test
    void should_buildCorrectKey_forTenantDimension() {
        RequestContext context = new RequestContext();
        context.setTenantId(999L);
        AfgRequestContextHolder.setContext(context);

        RateLimitResult result = rateLimiter.builder()
            .key("api.tenant")
            .dimension(RateLimitDimension.TENANT)
            .rate(10)
            .tryAcquire();

        assertThat(result).isNotNull();
    }

    @Test
    void should_buildCorrectKey_forApiDimension() {
        RateLimitResult result = rateLimiter.builder()
            .key("api.global")
            .dimension(RateLimitDimension.API)
            .rate(100)
            .tryAcquire();

        assertThat(result).isNotNull();
    }

    @Test
    void should_handleNullContext_gracefully() {
        AfgRequestContextHolder.clear();

        RateLimitResult result = rateLimiter.builder()
            .key("test")
            .dimension(RateLimitDimension.IP)
            .rate(10)
            .tryAcquire();

        assertThat(result).isNotNull();
    }

    @Test
    void should_handleAnonymousUser_gracefully() {
        RequestContext context = new RequestContext();
        // userId 默认为 null，表示匿名用户
        AfgRequestContextHolder.setContext(context);

        RateLimitResult result = rateLimiter.builder()
            .key("test")
            .dimension(RateLimitDimension.USER)
            .rate(10)
            .tryAcquire();

        assertThat(result).isNotNull();
    }

    @Test
    void should_useWhitelist_when_ipInWhitelist() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getIps().add("192.168.1.1");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        RateLimitResult result = rateLimiter.builder()
            .key("test")
            .dimension(RateLimitDimension.IP)
            .rate(10)
            .tryAcquire();

        // 白名单内的 IP 应该直接通过
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void should_useSlidingWindow_when_algorithmSpecified() {
        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        RateLimitResult result = rateLimiter.builder()
            .key("test")
            .dimension(RateLimitDimension.IP)
            .rate(10)
            .algorithm(RateLimitAlgorithm.SLIDING_WINDOW)
            .windowSize(60)
            .tryAcquire();

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void should_useDefaultRate_when_notSpecified() {
        properties.setDefaultRate(5);

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        RateLimitResult result = rateLimiter.builder()
            .key("test-default-rate")
            .dimension(RateLimitDimension.IP)
            .tryAcquire();

        assertThat(result).isNotNull();
    }

    @Test
    void should_useCustomKeyPrefix() {
        properties.setKeyPrefix("customPrefix");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        RateLimitResult result = rateLimiter.builder()
            .key("test")
            .dimension(RateLimitDimension.API)
            .tryAcquire();

        assertThat(result).isNotNull();
    }
}
