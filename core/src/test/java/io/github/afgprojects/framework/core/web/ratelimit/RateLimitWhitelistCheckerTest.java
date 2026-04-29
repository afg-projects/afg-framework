package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.github.afgprojects.framework.core.support.BaseUnitTest;
import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

class RateLimitWhitelistCheckerTest extends BaseUnitTest {

    private RateLimitProperties properties;
    private RateLimitWhitelistChecker checker;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        checker = new RateLimitWhitelistChecker(properties);

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
        properties.getWhitelist().setEnabled(false);
        properties.getWhitelist().getIps().add("192.168.1.1");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isFalse();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchExactIp() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getIps().add("192.168.1.1");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.1");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchWildcardIp() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getIps().add("192.168.1.*");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.100");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_notMatchDifferentIp() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getIps().add("192.168.1.1");

        RequestContext context = new RequestContext();
        context.setClientIp("192.168.1.2");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isFalse();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchUserId() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getUserIds().add(12345L);

        RequestContext context = new RequestContext();
        context.setUserId(12345L);
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.USER);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchUsername() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getUsernames().add("admin");

        RequestContext context = new RequestContext();
        context.setUsername("admin");
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.USER);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_matchTenantId() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getTenantIds().add(999L);

        RequestContext context = new RequestContext();
        context.setTenantId(999L);
        AfgRequestContextHolder.setContext(context);

        boolean result = checker.isInWhitelist(RateLimitDimension.TENANT);

        assertThat(result).isTrue();

        AfgRequestContextHolder.clear();
    }

    @Test
    void should_returnFalse_forApiDimension() {
        properties.getWhitelist().setEnabled(true);

        boolean result = checker.isInWhitelist(RateLimitDimension.API);

        assertThat(result).isFalse();
    }

    @Test
    void should_handleNullContext() {
        properties.getWhitelist().setEnabled(true);
        properties.getWhitelist().getIps().add("192.168.1.1");

        AfgRequestContextHolder.clear();

        boolean result = checker.isInWhitelist(RateLimitDimension.IP);

        assertThat(result).isFalse();
    }
}
