package io.github.afgprojects.framework.security.core.login.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CaptchaRequest 测试
 */
@DisplayName("CaptchaRequest 测试")
class CaptchaRequestTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("ofImage 应创建图形验证码请求")
        void shouldCreateImageCaptchaRequest() {
            CaptchaRequest request = CaptchaRequest.ofImage();

            assertThat(request.captchaType()).isEqualTo(CaptchaType.IMAGE);
            assertThat(request.target()).isNull();
            assertThat(request.tenantId()).isNull();
        }

        @Test
        @DisplayName("ofImage 带租户应创建图形验证码请求")
        void shouldCreateImageCaptchaRequestWithTenant() {
            CaptchaRequest request = CaptchaRequest.ofImage("tenant-001");

            assertThat(request.captchaType()).isEqualTo(CaptchaType.IMAGE);
            assertThat(request.target()).isNull();
            assertThat(request.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("ofSms 应创建短信验证码请求")
        void shouldCreateSmsCaptchaRequest() {
            CaptchaRequest request = CaptchaRequest.ofSms("13800138000");

            assertThat(request.captchaType()).isEqualTo(CaptchaType.SMS);
            assertThat(request.target()).isEqualTo("13800138000");
            assertThat(request.tenantId()).isNull();
        }

        @Test
        @DisplayName("ofSms 带租户应创建短信验证码请求")
        void shouldCreateSmsCaptchaRequestWithTenant() {
            CaptchaRequest request = CaptchaRequest.ofSms("13800138000", "tenant-001");

            assertThat(request.captchaType()).isEqualTo(CaptchaType.SMS);
            assertThat(request.target()).isEqualTo("13800138000");
            assertThat(request.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("ofEmail 应创建邮箱验证码请求")
        void shouldCreateEmailCaptchaRequest() {
            CaptchaRequest request = CaptchaRequest.ofEmail("test@example.com");

            assertThat(request.captchaType()).isEqualTo(CaptchaType.EMAIL);
            assertThat(request.target()).isEqualTo("test@example.com");
            assertThat(request.tenantId()).isNull();
        }

        @Test
        @DisplayName("ofEmail 带租户应创建邮箱验证码请求")
        void shouldCreateEmailCaptchaRequestWithTenant() {
            CaptchaRequest request = CaptchaRequest.ofEmail("test@example.com", "tenant-001");

            assertThat(request.captchaType()).isEqualTo(CaptchaType.EMAIL);
            assertThat(request.target()).isEqualTo("test@example.com");
            assertThat(request.tenantId()).isEqualTo("tenant-001");
        }
    }

    @Nested
    @DisplayName("record 特性")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            CaptchaRequest request1 = CaptchaRequest.ofSms("13800138000", "tenant-001");
            CaptchaRequest request2 = CaptchaRequest.ofSms("13800138000", "tenant-001");
            CaptchaRequest request3 = CaptchaRequest.ofSms("13900139000", "tenant-001");

            assertThat(request1).isEqualTo(request2);
            assertThat(request1).hasSameHashCodeAs(request2);
            assertThat(request1).isNotEqualTo(request3);
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            CaptchaRequest request = CaptchaRequest.ofSms("13800138000");

            String str = request.toString();

            assertThat(str).contains("CaptchaRequest");
            assertThat(str).contains("SMS");
            assertThat(str).contains("13800138000");
        }
    }
}
