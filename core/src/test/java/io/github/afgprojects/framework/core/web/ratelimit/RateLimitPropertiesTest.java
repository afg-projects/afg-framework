package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * RateLimitProperties 单元测试。
 * <p>
 * 测试限流配置属性的功能，验证默认配置、维度配置、白名单配置和响应头配置。
 *
 * @see AfgCoreProperties.RateLimitConfig
 */
class RateLimitPropertiesTest {

    /**
     * 测试创建时具有默认值。
     */
    @Test
    void should_haveDefaultValues_when_created() {
        AfgCoreProperties properties = new AfgCoreProperties();

        assertThat(properties.getRateLimit().isEnabled()).isTrue();
        assertThat(properties.getRateLimit().getDefaultRate()).isEqualTo(10);
        assertThat(properties.getRateLimit().getDefaultBurst()).isEqualTo(0);
        assertThat(properties.getRateLimit().getDefaultAlgorithm()).isEqualTo(AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.TOKEN_BUCKET);
        assertThat(properties.getRateLimit().getKeyPrefix()).isEqualTo("rateLimit");
        assertThat(properties.getRateLimit().getFallback().isEnabled()).isTrue();
        assertThat(properties.getRateLimit().getFallback().getDefaultMessage()).isEqualTo("请求过于频繁，请稍后再试");
    }

    @Test
    void should_allowSettingValues_when_modified() {
        AfgCoreProperties properties = new AfgCoreProperties();

        properties.getRateLimit().setEnabled(false);
        properties.getRateLimit().setDefaultRate(100);
        properties.getRateLimit().setDefaultBurst(200);
        properties.getRateLimit().setDefaultAlgorithm(AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.SLIDING_WINDOW);
        properties.getRateLimit().setKeyPrefix("customPrefix");
        properties.getRateLimit().getFallback().setEnabled(false);
        properties.getRateLimit().getFallback().setDefaultMessage("自定义消息");

        assertThat(properties.getRateLimit().isEnabled()).isFalse();
        assertThat(properties.getRateLimit().getDefaultRate()).isEqualTo(100);
        assertThat(properties.getRateLimit().getDefaultBurst()).isEqualTo(200);
        assertThat(properties.getRateLimit().getDefaultAlgorithm()).isEqualTo(AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.SLIDING_WINDOW);
        assertThat(properties.getRateLimit().getKeyPrefix()).isEqualTo("customPrefix");
        assertThat(properties.getRateLimit().getFallback().isEnabled()).isFalse();
        assertThat(properties.getRateLimit().getFallback().getDefaultMessage()).isEqualTo("自定义消息");
    }

    @Test
    void should_allowAddingDimensionConfigs() {
        AfgCoreProperties properties = new AfgCoreProperties();

        AfgCoreProperties.RateLimitConfig.DimensionConfig ipConfig = new AfgCoreProperties.RateLimitConfig.DimensionConfig();
        ipConfig.setRate(50);
        ipConfig.setBurst(100);
        ipConfig.setAlgorithm(AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.SLIDING_WINDOW);
        ipConfig.setWindowSize(60);

        properties.getRateLimit().getDimensions().put("ip", ipConfig);

        assertThat(properties.getRateLimit().getDimensions()).containsKey("ip");
        assertThat(properties.getRateLimit().getDimensions().get("ip").getRate()).isEqualTo(50);
        assertThat(properties.getRateLimit().getDimensions().get("ip").getBurst()).isEqualTo(100);
        assertThat(properties.getRateLimit().getDimensions().get("ip").getAlgorithm()).isEqualTo(AfgCoreProperties.RateLimitConfig.RateLimitAlgorithm.SLIDING_WINDOW);
        assertThat(properties.getRateLimit().getDimensions().get("ip").getWindowSize()).isEqualTo(60);
    }

    @Test
    void should_haveWhitelistConfig() {
        AfgCoreProperties properties = new AfgCoreProperties();

        assertThat(properties.getRateLimit().getWhitelist().isEnabled()).isTrue();
        assertThat(properties.getRateLimit().getWhitelist().getIps()).isEmpty();
        assertThat(properties.getRateLimit().getWhitelist().getUserIds()).isEmpty();
        assertThat(properties.getRateLimit().getWhitelist().getUsernames()).isEmpty();
        assertThat(properties.getRateLimit().getWhitelist().getTenantIds()).isEmpty();
    }

    @Test
    void should_allowAddingWhitelistEntries() {
        AfgCoreProperties properties = new AfgCoreProperties();

        properties.getRateLimit().getWhitelist().getIps().add("192.168.1.1");
        properties.getRateLimit().getWhitelist().getIps().add("10.0.0.*");
        properties.getRateLimit().getWhitelist().getUserIds().add(1L);
        properties.getRateLimit().getWhitelist().getUserIds().add(2L);
        properties.getRateLimit().getWhitelist().getUsernames().add("admin");
        properties.getRateLimit().getWhitelist().getTenantIds().add(100L);

        assertThat(properties.getRateLimit().getWhitelist().getIps()).containsExactly("192.168.1.1", "10.0.0.*");
        assertThat(properties.getRateLimit().getWhitelist().getUserIds()).containsExactly(1L, 2L);
        assertThat(properties.getRateLimit().getWhitelist().getUsernames()).containsExactly("admin");
        assertThat(properties.getRateLimit().getWhitelist().getTenantIds()).containsExactly(100L);
    }

    @Test
    void should_haveResponseHeadersConfig() {
        AfgCoreProperties properties = new AfgCoreProperties();

        assertThat(properties.getRateLimit().getResponseHeaders().isEnabled()).isTrue();
        assertThat(properties.getRateLimit().getResponseHeaders().getLimitHeader()).isEqualTo("X-RateLimit-Limit");
        assertThat(properties.getRateLimit().getResponseHeaders().getRemainingHeader()).isEqualTo("X-RateLimit-Remaining");
        assertThat(properties.getRateLimit().getResponseHeaders().getResetHeader()).isEqualTo("X-RateLimit-Reset");
        assertThat(properties.getRateLimit().getResponseHeaders().getRetryAfterHeader()).isEqualTo("Retry-After");
    }

    @Test
    void should_haveLocalRateLimitConfig() {
        AfgCoreProperties properties = new AfgCoreProperties();

        assertThat(properties.getRateLimit().getLocal().isEnabled()).isFalse();
        assertThat(properties.getRateLimit().getLocal().getCacheSize()).isEqualTo(10000);
        assertThat(properties.getRateLimit().getLocal().getExpireAfterSeconds()).isEqualTo(3600);
    }

    @Test
    void should_allowDisablingWhitelist() {
        AfgCoreProperties properties = new AfgCoreProperties();
        properties.getRateLimit().getWhitelist().setEnabled(false);

        assertThat(properties.getRateLimit().getWhitelist().isEnabled()).isFalse();
    }

    @Test
    void should_allowEnablingLocalRateLimit() {
        AfgCoreProperties properties = new AfgCoreProperties();
        properties.getRateLimit().getLocal().setEnabled(true);
        properties.getRateLimit().getLocal().setCacheSize(5000);
        properties.getRateLimit().getLocal().setExpireAfterSeconds(1800);

        assertThat(properties.getRateLimit().getLocal().isEnabled()).isTrue();
        assertThat(properties.getRateLimit().getLocal().getCacheSize()).isEqualTo(5000);
        assertThat(properties.getRateLimit().getLocal().getExpireAfterSeconds()).isEqualTo(1800);
    }
}
