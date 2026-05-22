package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.api.ratelimit.DefaultWhitelistStrategy;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitDimension;
import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

/**
 * RateLimitWhitelistChecker 单元测试。
 * <p>
 * 测试限流白名单检查器的功能，验证 IP、用户 ID、用户名和租户 ID 的白名单匹配。
 *
 * @see DefaultWhitelistStrategy
 */
class RateLimitWhitelistCheckerTest extends BaseUnitTest {

    private AfgCoreProperties properties;
    private DefaultWhitelistStrategy checker;

    @BeforeEach
    void setUp() {
        properties = new AfgCoreProperties();
        checker = new DefaultWhitelistStrategy(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        AfgRequestContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void should_returnFalse_when_whitelistDisabled() {
        properties.getRateLimit().getWhitelist().setEnabled(false);
        properties.getRateLimit().getWhitelist().getIps().add("192.168.1.1");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isFalse();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchExactIp() {
        properties.getRateLimit().getWhitelist().setEnabled(true);
        properties.getRateLimit().getWhitelist().getIps().add("192.168.1.1");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchWildcardIp() {
        properties.getRateLimit().getWhitelist().setEnabled(true);
        properties.getRateLimit().getWhitelist().getIps().add("192.168.1.*");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.100");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_notMatchDifferentIp() {
        properties.getRateLimit().getWhitelist().setEnabled(true);
        properties.getRateLimit().getWhitelist().getIps().add("192.168.1.1");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.2");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isFalse();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchUserId() {
        properties.getRateLimit().getWhitelist().setEnabled(true);
        properties.getRateLimit().getWhitelist().getUserIds().add(12345L);

        RequestContext context = new RequestContext();
        context.setUserId(12345L);
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.USER);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchUsername() {
        properties.getRateLimit().getWhitelist().setEnabled(true);
        properties.getRateLimit().getWhitelist().getUsernames().add("admin");

        RequestContext context = new RequestContext();
        context.setUsername("admin");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.USER);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchTenantId() {
        properties.getRateLimit().getWhitelist().setEnabled(true);
        properties.getRateLimit().getWhitelist().getTenantIds().add(999L);

        RequestContext context = new RequestContext();
        context.setTenantId(999L);
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.TENANT);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_returnFalse_forApiDimension() {
        properties.getRateLimit().getWhitelist().setEnabled(true);

        boolean result = checker.isInWhitelist(RateLimitDimension.API);

        assertThat(result).isFalse();
    }

    @Test
    void should_handleNullContext() {
        properties.getRateLimit().getWhitelist().setEnabled(true);
        properties.getRateLimit().getWhitelist().getIps().add("192.168.1.1");

        AfgRequestContextHolder.clear();

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isFalse();
    }
}
