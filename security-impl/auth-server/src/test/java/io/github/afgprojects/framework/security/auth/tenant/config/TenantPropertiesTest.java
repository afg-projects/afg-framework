package io.github.afgprojects.framework.security.auth.tenant.config;

import io.github.afgprojects.framework.security.auth.autoconfigure.AuthSecurityProperties;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class TenantPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getStrategies()).containsExactly(
            AuthSecurityProperties.TenantConfig.TenantStrategy.TOKEN,
            AuthSecurityProperties.TenantConfig.TenantStrategy.HEADER,
            AuthSecurityProperties.TenantConfig.TenantStrategy.DOMAIN,
            AuthSecurityProperties.TenantConfig.TenantStrategy.DEFAULT
        );
        assertThat(properties.getDefaultTenant()).isEqualTo("default");
        assertThat(properties.isFailIfUnresolved()).isFalse();
        assertThat(properties.getHeaderName()).isEqualTo("X-Tenant-Id");
        assertThat(properties.getDomainMappings()).isEmpty();
        assertThat(properties.getValidation().isEnabled()).isTrue();
        assertThat(properties.getValidation().getCacheTtl()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldSetProperties() {
        AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();
        properties.setEnabled(false);
        properties.setStrategies(List.of(AuthSecurityProperties.TenantConfig.TenantStrategy.HEADER));
        properties.setDefaultTenant("custom-default");
        properties.setFailIfUnresolved(true);
        properties.setHeaderName("X-Custom-Tenant");
        properties.setDomainMappings(Map.of("tenant.example.com", "tenant-001"));

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getStrategies()).containsExactly(AuthSecurityProperties.TenantConfig.TenantStrategy.HEADER);
        assertThat(properties.getDefaultTenant()).isEqualTo("custom-default");
        assertThat(properties.isFailIfUnresolved()).isTrue();
        assertThat(properties.getHeaderName()).isEqualTo("X-Custom-Tenant");
        assertThat(properties.getDomainMappings()).containsEntry("tenant.example.com", "tenant-001");
    }

    @Test
    void shouldSetValidationConfig() {
        AuthSecurityProperties.TenantConfig properties = new AuthSecurityProperties.TenantConfig();
        properties.getValidation().setEnabled(false);
        properties.getValidation().setCacheTtl(Duration.ofMinutes(10));

        assertThat(properties.getValidation().isEnabled()).isFalse();
        assertThat(properties.getValidation().getCacheTtl()).isEqualTo(Duration.ofMinutes(10));
    }
}
