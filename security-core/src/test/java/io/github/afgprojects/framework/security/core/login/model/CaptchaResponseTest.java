package io.github.afgprojects.framework.security.core.login.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CaptchaResponse 测试
 */
@DisplayName("CaptchaResponse 测试")
class CaptchaResponseTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("应使用 Builder 构建完整响应")
        void shouldBuildCompleteResponse() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("key-123")
                    .captchaImage("base64-image-data")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300)
                    .build();

            assertThat(response.captchaKey()).isEqualTo("key-123");
            assertThat(response.captchaImage()).isEqualTo("base64-image-data");
            assertThat(response.captchaType()).isEqualTo(CaptchaType.IMAGE);
            assertThat(response.expiresIn()).isEqualTo(300);
        }

        @Test
        @DisplayName("Builder 应使用默认过期时间")
        void shouldUseDefaultExpiresIn() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("key-123")
                    .captchaType(CaptchaType.SMS)
                    .build();

            assertThat(response.expiresIn()).isEqualTo(300);
        }

        @Test
        @DisplayName("captchaImage 可为 null")
        void shouldAllowNullCaptchaImage() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("key-123")
                    .captchaType(CaptchaType.SMS)
                    .build();

            assertThat(response.captchaImage()).isNull();
        }
    }

    @Nested
    @DisplayName("record 特性")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            CaptchaResponse response1 = CaptchaResponse.builder()
                    .captchaKey("key-123")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300)
                    .build();

            CaptchaResponse response2 = CaptchaResponse.builder()
                    .captchaKey("key-123")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300)
                    .build();

            CaptchaResponse response3 = CaptchaResponse.builder()
                    .captchaKey("key-456")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300)
                    .build();

            assertThat(response1).isEqualTo(response2);
            assertThat(response1).hasSameHashCodeAs(response2);
            assertThat(response1).isNotEqualTo(response3);
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("key-123")
                    .captchaType(CaptchaType.IMAGE)
                    .build();

            String str = response.toString();

            assertThat(str).contains("CaptchaResponse");
            assertThat(str).contains("key-123");
            assertThat(str).contains("IMAGE");
        }
    }
}
