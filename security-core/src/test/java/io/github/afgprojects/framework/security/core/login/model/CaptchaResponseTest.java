package io.github.afgprojects.framework.security.core.login.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CaptchaResponseTest {

    @Nested
    @DisplayName("Builder 模式测试")
    class BuilderTests {

        @Test
        @DisplayName("应使用 Builder 构建完整响应")
        void shouldBuildCompleteResponse() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("captcha-key-001")
                    .captchaImage("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300L)
                    .build();

            assertThat(response.captchaKey()).isEqualTo("captcha-key-001");
            assertThat(response.captchaImage()).startsWith("data:image/png;base64,");
            assertThat(response.captchaType()).isEqualTo(CaptchaType.IMAGE);
            assertThat(response.expiresIn()).isEqualTo(300L);
        }

        @Test
        @DisplayName("应使用 Builder 构建最小响应")
        void shouldBuildMinimalResponse() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("captcha-key-001")
                    .captchaType(CaptchaType.IMAGE)
                    .build();

            assertThat(response.captchaKey()).isEqualTo("captcha-key-001");
            assertThat(response.captchaType()).isEqualTo(CaptchaType.IMAGE);
        }
    }

    @Nested
    @DisplayName("图形验证码响应测试")
    class ImageCaptchaTests {

        @Test
        @DisplayName("应构建图形验证码响应")
        void shouldBuildImageCaptchaResponse() {
            String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("img-captcha-001")
                    .captchaImage(base64Image)
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(180L)
                    .build();

            assertThat(response.captchaKey()).isEqualTo("img-captcha-001");
            assertThat(response.captchaImage()).isEqualTo(base64Image);
            assertThat(response.captchaType()).isEqualTo(CaptchaType.IMAGE);
            assertThat(response.expiresIn()).isEqualTo(180L);
        }

        @Test
        @DisplayName("图形验证码应包含图片")
        void imageCaptchaShouldHaveImage() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("img-captcha-001")
                    .captchaImage("base64-encoded-image")
                    .captchaType(CaptchaType.IMAGE)
                    .build();

            assertThat(response.captchaImage()).isNotNull();
            assertThat(response.captchaImage()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("短信验证码响应测试")
    class SmsCaptchaTests {

        @Test
        @DisplayName("应构建短信验证码响应")
        void shouldBuildSmsCaptchaResponse() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("sms-captcha-001")
                    .captchaType(CaptchaType.SMS)
                    .expiresIn(300L)
                    .build();

            assertThat(response.captchaKey()).isEqualTo("sms-captcha-001");
            assertThat(response.captchaType()).isEqualTo(CaptchaType.SMS);
            assertThat(response.expiresIn()).isEqualTo(300L);
        }

        @Test
        @DisplayName("短信验证码无图片")
        void smsCaptchaShouldNotHaveImage() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("sms-captcha-001")
                    .captchaType(CaptchaType.SMS)
                    .build();

            assertThat(response.captchaImage()).isNull();
        }
    }

    @Nested
    @DisplayName("邮箱验证码响应测试")
    class EmailCaptchaTests {

        @Test
        @DisplayName("应构建邮箱验证码响应")
        void shouldBuildEmailCaptchaResponse() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("email-captcha-001")
                    .captchaType(CaptchaType.EMAIL)
                    .expiresIn(600L)
                    .build();

            assertThat(response.captchaKey()).isEqualTo("email-captcha-001");
            assertThat(response.captchaType()).isEqualTo(CaptchaType.EMAIL);
            assertThat(response.expiresIn()).isEqualTo(600L);
        }

        @Test
        @DisplayName("邮箱验证码无图片")
        void emailCaptchaShouldNotHaveImage() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("email-captcha-001")
                    .captchaType(CaptchaType.EMAIL)
                    .build();

            assertThat(response.captchaImage()).isNull();
        }
    }

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("默认过期时间应为 300 秒")
        void defaultExpiresInShouldBe300() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("captcha-key-001")
                    .captchaType(CaptchaType.IMAGE)
                    .build();

            assertThat(response.expiresIn()).isEqualTo(300L);
        }

        @Test
        @DisplayName("应允许覆盖默认过期时间")
        void shouldAllowOverrideExpiresIn() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("captcha-key-001")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(600L)
                    .build();

            assertThat(response.expiresIn()).isEqualTo(600L);
        }
    }

    @Nested
    @DisplayName("验证码类型枚举测试")
    class CaptchaTypeTests {

        @Test
        @DisplayName("应包含所有验证码类型")
        void shouldContainAllCaptchaTypes() {
            CaptchaType[] types = CaptchaType.values();

            assertThat(types).hasSize(3);
            assertThat(types).contains(CaptchaType.IMAGE, CaptchaType.SMS, CaptchaType.EMAIL);
        }
    }

    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals")
        void shouldImplementEquals() {
            CaptchaResponse response1 = CaptchaResponse.builder()
                    .captchaKey("captcha-key-001")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300L)
                    .build();

            CaptchaResponse response2 = CaptchaResponse.builder()
                    .captchaKey("captcha-key-001")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300L)
                    .build();

            assertThat(response1).isEqualTo(response2);
        }

        @Test
        @DisplayName("应正确实现 hashCode")
        void shouldImplementHashCode() {
            CaptchaResponse response1 = CaptchaResponse.builder()
                    .captchaKey("captcha-key-001")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300L)
                    .build();

            CaptchaResponse response2 = CaptchaResponse.builder()
                    .captchaKey("captcha-key-001")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300L)
                    .build();

            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            CaptchaResponse response = CaptchaResponse.builder()
                    .captchaKey("captcha-key-001")
                    .captchaType(CaptchaType.IMAGE)
                    .expiresIn(300L)
                    .build();

            String str = response.toString();

            assertThat(str).contains("CaptchaResponse");
            assertThat(str).contains("captcha-key-001");
            assertThat(str).contains("IMAGE");
        }
    }
}
