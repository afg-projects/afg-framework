package io.github.afgprojects.framework.security.resource.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ResourceServerProperties 测试类。
 *
 * @since 1.0.0
 */
class ResourceServerPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("enabled 默认值为 true")
        void shouldDefaultEnabledToTrue() {
            ResourceServerProperties properties = new ResourceServerProperties();
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("tenantStrategies 默认值为 [token, header]")
        void shouldDefaultTenantStrategies() {
            ResourceServerProperties properties = new ResourceServerProperties();
            assertThat(properties.getTenantStrategies()).containsExactly("token", "header");
        }

        @Test
        @DisplayName("tenantHeaderName 默认值为 X-Tenant-Id")
        void shouldDefaultTenantHeaderName() {
            ResourceServerProperties properties = new ResourceServerProperties();
            assertThat(properties.getTenantHeaderName()).isEqualTo("X-Tenant-Id");
        }

        @Test
        @DisplayName("failIfTenantUnresolved 默认值为 true")
        void shouldDefaultFailIfTenantUnresolvedToTrue() {
            ResourceServerProperties properties = new ResourceServerProperties();
            assertThat(properties.isFailIfTenantUnresolved()).isTrue();
        }
    }

    @Nested
    @DisplayName("setter 测试")
    class SetterTests {

        @Test
        @DisplayName("设置 enabled")
        void shouldSetEnabled() {
            ResourceServerProperties properties = new ResourceServerProperties();
            properties.setEnabled(false);
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("设置 tenantStrategies")
        void shouldSetTenantStrategies() {
            ResourceServerProperties properties = new ResourceServerProperties();
            properties.setTenantStrategies(List.of("header", "domain"));
            assertThat(properties.getTenantStrategies()).containsExactly("header", "domain");
        }

        @Test
        @DisplayName("设置 tenantHeaderName")
        void shouldSetTenantHeaderName() {
            ResourceServerProperties properties = new ResourceServerProperties();
            properties.setTenantHeaderName("X-Custom-Tenant");
            assertThat(properties.getTenantHeaderName()).isEqualTo("X-Custom-Tenant");
        }

        @Test
        @DisplayName("设置 failIfTenantUnresolved")
        void shouldSetFailIfTenantUnresolved() {
            ResourceServerProperties properties = new ResourceServerProperties();
            properties.setFailIfTenantUnresolved(false);
            assertThat(properties.isFailIfTenantUnresolved()).isFalse();
        }
    }
}