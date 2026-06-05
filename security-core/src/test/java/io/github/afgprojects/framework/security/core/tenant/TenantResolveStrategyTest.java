package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantResolveStrategy 测试
 */
@DisplayName("TenantResolveStrategy 测试")
class TenantResolveStrategyTest {

    @Nested
    @DisplayName("枚举值")
    class EnumValueTests {

        @Test
        @DisplayName("应包含所有策略")
        void shouldContainAllStrategies() {
            TenantResolveStrategy[] strategies = TenantResolveStrategy.values();

            assertThat(strategies).hasSize(4);
            assertThat(strategies).containsExactlyInAnyOrder(
                    TenantResolveStrategy.TOKEN,
                    TenantResolveStrategy.HEADER,
                    TenantResolveStrategy.DOMAIN,
                    TenantResolveStrategy.DEFAULT
            );
        }
    }

    @Nested
    @DisplayName("优先级顺序")
    class OrderTests {

        @Test
        @DisplayName("TOKEN 优先级最高")
        void shouldHaveHighestPriorityForToken() {
            assertThat(TenantResolveStrategy.TOKEN.getOrder()).isEqualTo(100);
        }

        @Test
        @DisplayName("HEADER 优先级次之")
        void shouldHaveSecondPriorityForHeader() {
            assertThat(TenantResolveStrategy.HEADER.getOrder()).isEqualTo(200);
        }

        @Test
        @DisplayName("DOMAIN 优先级第三")
        void shouldHaveThirdPriorityForDomain() {
            assertThat(TenantResolveStrategy.DOMAIN.getOrder()).isEqualTo(300);
        }

        @Test
        @DisplayName("DEFAULT 优先级最低")
        void shouldHaveLowestPriorityForDefault() {
            assertThat(TenantResolveStrategy.DEFAULT.getOrder()).isEqualTo(400);
        }

        @Test
        @DisplayName("优先级应按 TOKEN < HEADER < DOMAIN < DEFAULT 递增")
        void shouldHaveIncreasingOrder() {
            assertThat(TenantResolveStrategy.TOKEN.getOrder())
                    .isLessThan(TenantResolveStrategy.HEADER.getOrder());
            assertThat(TenantResolveStrategy.HEADER.getOrder())
                    .isLessThan(TenantResolveStrategy.DOMAIN.getOrder());
            assertThat(TenantResolveStrategy.DOMAIN.getOrder())
                    .isLessThan(TenantResolveStrategy.DEFAULT.getOrder());
        }
    }

    @Nested
    @DisplayName("描述")
    class DescriptionTests {

        @Test
        @DisplayName("每个策略应有非空描述")
        void shouldHaveNonNullDescription() {
            for (TenantResolveStrategy strategy : TenantResolveStrategy.values()) {
                assertThat(strategy.getDescription()).isNotNull();
                assertThat(strategy.getDescription()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("TOKEN 描述应包含 token")
        void shouldContainTokenInTokenDescription() {
            assertThat(TenantResolveStrategy.TOKEN.getDescription()).containsIgnoringCase("token");
        }

        @Test
        @DisplayName("HEADER 描述应包含 header")
        void shouldContainHeaderInHeaderDescription() {
            assertThat(TenantResolveStrategy.HEADER.getDescription()).containsIgnoringCase("header");
        }

        @Test
        @DisplayName("DOMAIN 描述应包含 domain 或 subdomain")
        void shouldContainDomainInDomainDescription() {
            String desc = TenantResolveStrategy.DOMAIN.getDescription();
            assertThat(desc.toLowerCase()).containsAnyOf("domain", "subdomain");
        }

        @Test
        @DisplayName("DEFAULT 描述应包含 default")
        void shouldContainDefaultInDefaultDescription() {
            assertThat(TenantResolveStrategy.DEFAULT.getDescription()).containsIgnoringCase("default");
        }
    }
}
