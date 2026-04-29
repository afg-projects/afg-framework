package io.github.afgprojects.framework.core.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimitPropertiesTest {

    @Test
    void should_haveDefaultValues_when_created() {
        RateLimitProperties properties = new RateLimitProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getDefaultRate()).isEqualTo(10);
        assertThat(properties.getDefaultBurst()).isEqualTo(0);
        assertThat(properties.getDefaultAlgorithm()).isEqualTo(RateLimitAlgorithm.TOKEN_BUCKET);
        assertThat(properties.getKeyPrefix()).isEqualTo("rateLimit");
        assertThat(properties.getFallback().isEnabled()).isTrue();
        assertThat(properties.getFallback().getDefaultMessage()).isEqualTo("请求过于频繁，请稍后再试");
    }

    @Test
    void should_allowSettingValues_when_modified() {
        RateLimitProperties properties = new RateLimitProperties();

        properties.setEnabled(false);
        properties.setDefaultRate(100);
        properties.setDefaultBurst(200);
        properties.setDefaultAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW);
        properties.setKeyPrefix("customPrefix");
        properties.getFallback().setEnabled(false);
        properties.getFallback().setDefaultMessage("自定义消息");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getDefaultRate()).isEqualTo(100);
        assertThat(properties.getDefaultBurst()).isEqualTo(200);
        assertThat(properties.getDefaultAlgorithm()).isEqualTo(RateLimitAlgorithm.SLIDING_WINDOW);
        assertThat(properties.getKeyPrefix()).isEqualTo("customPrefix");
        assertThat(properties.getFallback().isEnabled()).isFalse();
        assertThat(properties.getFallback().getDefaultMessage()).isEqualTo("自定义消息");
    }

    @Test
    void should_allowAddingDimensionConfigs() {
        RateLimitProperties properties = new RateLimitProperties();

        RateLimitProperties.DimensionConfig ipConfig = new RateLimitProperties.DimensionConfig();
        ipConfig.setRate(50);
        ipConfig.setBurst(100);
        ipConfig.setAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW);
        ipConfig.setWindowSize(60);

        properties.getDimensions().put("ip", ipConfig);

        assertThat(properties.getDimensions()).containsKey("ip");
        assertThat(properties.getDimensions().get("ip").getRate()).isEqualTo(50);
        assertThat(properties.getDimensions().get("ip").getBurst()).isEqualTo(100);
        assertThat(properties.getDimensions().get("ip").getAlgorithm()).isEqualTo(RateLimitAlgorithm.SLIDING_WINDOW);
        assertThat(properties.getDimensions().get("ip").getWindowSize()).isEqualTo(60);
    }

    @Test
    void should_haveWhitelistConfig() {
        RateLimitProperties properties = new RateLimitProperties();

        assertThat(properties.getWhitelist().isEnabled()).isTrue();
        assertThat(properties.getWhitelist().getIps()).isEmpty();
        assertThat(properties.getWhitelist().getUserIds()).isEmpty();
        assertThat(properties.getWhitelist().getUsernames()).isEmpty();
        assertThat(properties.getWhitelist().getTenantIds()).isEmpty();
    }

    @Test
    void should_allowAddingWhitelistEntries() {
        RateLimitProperties properties = new RateLimitProperties();

        properties.getWhitelist().getIps().add("192.168.1.1");
        properties.getWhitelist().getIps().add("10.0.0.*");
        properties.getWhitelist().getUserIds().add(1L);
        properties.getWhitelist().getUserIds().add(2L);
        properties.getWhitelist().getUsernames().add("admin");
        properties.getWhitelist().getTenantIds().add(100L);

        assertThat(properties.getWhitelist().getIps()).containsExactly("192.168.1.1", "10.0.0.*");
        assertThat(properties.getWhitelist().getUserIds()).containsExactly(1L, 2L);
        assertThat(properties.getWhitelist().getUsernames()).containsExactly("admin");
        assertThat(properties.getWhitelist().getTenantIds()).containsExactly(100L);
    }

    @Test
    void should_haveResponseHeadersConfig() {
        RateLimitProperties properties = new RateLimitProperties();

        assertThat(properties.getResponseHeaders().isEnabled()).isTrue();
        assertThat(properties.getResponseHeaders().getLimitHeader()).isEqualTo("X-RateLimit-Limit");
        assertThat(properties.getResponseHeaders().getRemainingHeader()).isEqualTo("X-RateLimit-Remaining");
        assertThat(properties.getResponseHeaders().getResetHeader()).isEqualTo("X-RateLimit-Reset");
        assertThat(properties.getResponseHeaders().getRetryAfterHeader()).isEqualTo("Retry-After");
    }

    @Test
    void should_haveLocalRateLimitConfig() {
        RateLimitProperties properties = new RateLimitProperties();

        assertThat(properties.getLocal().isEnabled()).isFalse();
        assertThat(properties.getLocal().getCacheSize()).isEqualTo(10000);
        assertThat(properties.getLocal().getExpireAfterSeconds()).isEqualTo(3600);
    }

    @Test
    void should_haveMetricsConfig() {
        RateLimitProperties properties = new RateLimitProperties();

        assertThat(properties.getMetrics().isEnabled()).isTrue();
        assertThat(properties.getMetrics().getPrefix()).isEqualTo("afg.rate.limit");
    }

    @Test
    void should_allowDisablingWhitelist() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getWhitelist().setEnabled(false);

        assertThat(properties.getWhitelist().isEnabled()).isFalse();
    }

    @Test
    void should_allowEnablingLocalRateLimit() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getLocal().setEnabled(true);
        properties.getLocal().setCacheSize(5000);
        properties.getLocal().setExpireAfterSeconds(1800);

        assertThat(properties.getLocal().isEnabled()).isTrue();
        assertThat(properties.getLocal().getCacheSize()).isEqualTo(5000);
        assertThat(properties.getLocal().getExpireAfterSeconds()).isEqualTo(1800);
    }
}
