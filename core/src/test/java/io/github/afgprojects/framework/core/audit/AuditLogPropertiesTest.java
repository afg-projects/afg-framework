package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * AuditLogProperties 单元测试。
 * <p>
 * 测试审计日志配置属性的默认值和属性修改功能。
 *
 * @see AfgCoreProperties.AuditConfig
 */
class AuditLogPropertiesTest {

    /**
     * 测试配置属性是否具有正确的默认值。
     */
    @Test
    void should_haveDefaultValues() {
        // When
        AfgCoreProperties.AuditConfig properties = new AfgCoreProperties.AuditConfig();

        // Then
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getStorageType()).isEqualTo(AfgCoreProperties.AuditConfig.StorageType.LOG);
        assertThat(properties.getMaxSize()).isEqualTo(10000);
        assertThat(properties.getTtl()).isEqualTo(Duration.ofDays(7));
        assertThat(properties.isMultiTenant()).isTrue();
        assertThat(properties.getSensitiveFields()).containsExactly(
                "password", "token", "secret", "apikey", "credential", "accesstoken");
    }

    /**
     * 测试配置属性是否允许修改值。
     */
    @Test
    void should_allowChangingValues() {
        // Given
        AfgCoreProperties.AuditConfig properties = new AfgCoreProperties.AuditConfig();

        // When
        properties.setEnabled(false);
        properties.setStorageType(AfgCoreProperties.AuditConfig.StorageType.REDIS);
        properties.setMaxSize(5000);
        properties.setTtl(Duration.ofDays(30));
        properties.setMultiTenant(false);
        properties.setSensitiveFields(new String[]{"password", "secret"});

        // Then
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getStorageType()).isEqualTo(AfgCoreProperties.AuditConfig.StorageType.REDIS);
        assertThat(properties.getMaxSize()).isEqualTo(5000);
        assertThat(properties.getTtl()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.isMultiTenant()).isFalse();
        assertThat(properties.getSensitiveFields()).containsExactly("password", "secret");
    }
}
