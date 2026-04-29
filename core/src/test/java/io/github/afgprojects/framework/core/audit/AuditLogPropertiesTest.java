package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * AuditLogProperties 单元测试
 */
class AuditLogPropertiesTest {

    @Test
    void should_haveDefaultValues() {
        // When
        AuditLogProperties properties = new AuditLogProperties();

        // Then
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getStorageType()).isEqualTo(AuditLogProperties.StorageType.LOG);
        assertThat(properties.getMaxSize()).isEqualTo(10000);
        assertThat(properties.getTtl()).isEqualTo(Duration.ofDays(7));
        assertThat(properties.isMultiTenant()).isTrue();
        assertThat(properties.getSensitiveFields()).containsExactly(
                "password", "token", "secret", "apikey", "credential", "accesstoken");
    }

    @Test
    void should_allowChangingValues() {
        // Given
        AuditLogProperties properties = new AuditLogProperties();

        // When
        properties.setEnabled(false);
        properties.setStorageType(AuditLogProperties.StorageType.REDIS);
        properties.setMaxSize(5000);
        properties.setTtl(Duration.ofDays(30));
        properties.setMultiTenant(false);
        properties.setSensitiveFields(new String[]{"password", "secret"});

        // Then
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getStorageType()).isEqualTo(AuditLogProperties.StorageType.REDIS);
        assertThat(properties.getMaxSize()).isEqualTo(5000);
        assertThat(properties.getTtl()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.isMultiTenant()).isFalse();
        assertThat(properties.getSensitiveFields()).containsExactly("password", "secret");
    }
}
