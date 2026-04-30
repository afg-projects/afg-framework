package io.github.afgprojects.framework.core.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptionProperties 测试
 */
@DisplayName("EncryptionProperties 测试")
class EncryptionPropertiesTest {

    @Test
    @DisplayName("应该使用默认值创建 EncryptionProperties")
    void shouldCreateWithDefaults() {
        EncryptionProperties properties = new EncryptionProperties();
        assertFalse(properties.isEnabled());
        assertEquals("AES-256-GCM", properties.getAlgorithm());
        assertNull(properties.getSecretKey());
        assertEquals("ENC(", properties.getPrefix());
        assertEquals(")", properties.getSuffix());
    }

    @Test
    @DisplayName("应该正确设置属性")
    void shouldSetProperties() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setEnabled(true);
        properties.setAlgorithm("AES-128-CBC");
        properties.setSecretKey("my-secret-key");
        properties.setPrefix("[ENC]");
        properties.setSuffix("[/ENC]");

        assertTrue(properties.isEnabled());
        assertEquals("AES-128-CBC", properties.getAlgorithm());
        assertEquals("my-secret-key", properties.getSecretKey());
        assertEquals("[ENC]", properties.getPrefix());
        assertEquals("[/ENC]", properties.getSuffix());
    }
}
