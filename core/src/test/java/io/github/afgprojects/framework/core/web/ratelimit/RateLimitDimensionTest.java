package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.redisson.api.RateType;

/**
 * 限流维度测试
 */
class RateLimitDimensionTest {

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

    @Test
    void should_supportRateType() {
        // 验证 Redisson RateType 可用于限流配置
        assertThat(RateType.OVERALL).isNotNull();
        assertThat(RateType.PER_CLIENT).isNotNull();
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
