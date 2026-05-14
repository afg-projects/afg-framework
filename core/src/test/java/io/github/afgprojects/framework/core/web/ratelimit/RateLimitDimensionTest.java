package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.api.ratelimit.RateLimitDimension;

/**
 * RateLimitDimension 枚举单元测试。
 * <p>
 * 测试限流维度枚举的功能，验证 IP、USER、TENANT 和 API 维度的存在和使用。
 *
 * @see io.github.afgprojects.framework.core.api.ratelimit.RateLimitDimension
 */
class RateLimitDimensionTest {

    /**
     * 测试包含所有维度。
     */
    @Test
    void should_containAllDimensions() {
        RateLimitDimension[] dimensions = RateLimitDimension.values();

        assertThat(dimensions).hasSize(4);
        assertThat(dimensions)
                .containsExactly(
                        RateLimitDimension.IP,
                        RateLimitDimension.USER,
                        RateLimitDimension.TENANT,
                        RateLimitDimension.API);
    }

    @Test
    void should_haveCorrectNames() {
        assertThat(RateLimitDimension.IP.name()).isEqualTo("IP");
        assertThat(RateLimitDimension.USER.name()).isEqualTo("USER");
        assertThat(RateLimitDimension.TENANT.name()).isEqualTo("TENANT");
        assertThat(RateLimitDimension.API.name()).isEqualTo("API");
    }

    @Test
    void should_beUsableInAnnotation() throws NoSuchMethodException {
        Method method = TestService.class.getMethod("ipLimited");
        RateLimit annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.dimension()).isEqualTo(RateLimitDimension.IP);

        method = TestService.class.getMethod("userLimited");
        annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.dimension()).isEqualTo(RateLimitDimension.USER);

        method = TestService.class.getMethod("tenantLimited");
        annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.dimension()).isEqualTo(RateLimitDimension.TENANT);

        method = TestService.class.getMethod("apiLimited");
        annotation = method.getAnnotation(RateLimit.class);
        assertThat(annotation.dimension()).isEqualTo(RateLimitDimension.API);
    }

    // 测试服务类
    static class TestService {

        @RateLimit(key = "test.ip", dimension = RateLimitDimension.IP)
        public void ipLimited() {}

        @RateLimit(key = "test.user", dimension = RateLimitDimension.USER)
        public void userLimited() {}

        @RateLimit(key = "test.tenant", dimension = RateLimitDimension.TENANT)
        public void tenantLimited() {}

        @RateLimit(key = "test.api", dimension = RateLimitDimension.API)
        public void apiLimited() {}
    }
}
