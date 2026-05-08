package io.github.afgprojects.framework.security.resource.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TenantResolveStrategy 测试类。
 *
 * @since 1.0.0
 */
class TenantResolveStrategyTest {

    @Test
    @DisplayName("TOKEN 的 order 应为 100")
    void tokenOrderShouldBe100() {
        assertThat(TenantResolveStrategy.TOKEN.getOrder()).isEqualTo(100);
    }

    @Test
    @DisplayName("HEADER 的 order 应为 200")
    void headerOrderShouldBe200() {
        assertThat(TenantResolveStrategy.HEADER.getOrder()).isEqualTo(200);
    }

    @Test
    @DisplayName("DOMAIN 的 order 应为 300")
    void domainOrderShouldBe300() {
        assertThat(TenantResolveStrategy.DOMAIN.getOrder()).isEqualTo(300);
    }

    @Test
    @DisplayName("PATH 的 order 应为 400")
    void pathOrderShouldBe400() {
        assertThat(TenantResolveStrategy.PATH.getOrder()).isEqualTo(400);
    }

    @Test
    @DisplayName("TOKEN 的描述应正确")
    void tokenDescriptionShouldBeCorrect() {
        assertThat(TenantResolveStrategy.TOKEN.getDescription())
                .isEqualTo("Resolve tenant from JWT token");
    }

    @Test
    @DisplayName("HEADER 的描述应正确")
    void headerDescriptionShouldBeCorrect() {
        assertThat(TenantResolveStrategy.HEADER.getDescription())
                .isEqualTo("Resolve tenant from request header");
    }

    @Test
    @DisplayName("DOMAIN 的描述应正确")
    void domainDescriptionShouldBeCorrect() {
        assertThat(TenantResolveStrategy.DOMAIN.getDescription())
                .isEqualTo("Resolve tenant from subdomain");
    }

    @Test
    @DisplayName("PATH 的描述应正确")
    void pathDescriptionShouldBeCorrect() {
        assertThat(TenantResolveStrategy.PATH.getDescription())
                .isEqualTo("Resolve tenant from URL path");
    }

    @Test
    @DisplayName("所有策略的优先级顺序应正确")
    void allStrategiesShouldHaveCorrectOrder() {
        assertThat(TenantResolveStrategy.TOKEN.getOrder())
                .isLessThan(TenantResolveStrategy.HEADER.getOrder());
        assertThat(TenantResolveStrategy.HEADER.getOrder())
                .isLessThan(TenantResolveStrategy.DOMAIN.getOrder());
        assertThat(TenantResolveStrategy.DOMAIN.getOrder())
                .isLessThan(TenantResolveStrategy.PATH.getOrder());
    }
}