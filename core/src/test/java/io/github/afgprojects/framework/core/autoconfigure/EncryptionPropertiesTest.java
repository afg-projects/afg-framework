package io.github.afgprojects.framework.core.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * EncryptionProperties 测试
 */
@DisplayName("EncryptionProperties 测试")
class EncryptionPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            EncryptionProperties props = new EncryptionProperties();

            assertThat(props.isEnabled()).isFalse();
            assertThat(props.getAlgorithm()).isEqualTo("AES-256-GCM");
            assertThat(props.getSecretKey()).isNull();
            assertThat(props.getPrefix()).isEqualTo("ENC(");
            assertThat(props.getSuffix()).isEqualTo(")");
        }
    }

    @Nested
    @DisplayName("设置属性测试")
    class SetPropertiesTests {

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            EncryptionProperties props = new EncryptionProperties();
            props.setEnabled(true);
            props.setAlgorithm("AES-128-GCM");
            props.setSecretKey("my-secret-key");
            props.setPrefix("CRYPT(");
            props.setSuffix(")");

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getAlgorithm()).isEqualTo("AES-128-GCM");
            assertThat(props.getSecretKey()).isEqualTo("my-secret-key");
            assertThat(props.getPrefix()).isEqualTo("CRYPT(");
        }
    }
}
