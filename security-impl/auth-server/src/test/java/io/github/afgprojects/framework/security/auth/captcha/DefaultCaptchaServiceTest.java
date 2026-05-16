package io.github.afgprojects.framework.security.auth.captcha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Base64;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.security.core.login.CaptchaService;
import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.CaptchaType;
import io.github.afgprojects.framework.security.core.storage.AfgCaptchaStorage;

/**
 * DefaultCaptchaService 测试
 */
@DisplayName("DefaultCaptchaService 测试")
@ExtendWith(MockitoExtension.class)
class DefaultCaptchaServiceTest {

    @Mock
    private AfgCaptchaStorage captchaStorage;

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new DefaultCaptchaService(captchaStorage);
    }

    @Nested
    @DisplayName("生成图形验证码测试")
    class GenerateCaptchaTests {

        @Test
        @DisplayName("应该生成图形验证码")
        void shouldGenerateImageCaptcha() {
            // given
            CaptchaRequest request = CaptchaRequest.ofImage();

            // when
            CaptchaResponse response = captchaService.generate(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.captchaKey()).isNotBlank();
            assertThat(response.captchaImage()).isNotBlank();
            assertThat(response.captchaType()).isEqualTo(CaptchaType.IMAGE);
            assertThat(response.expiresIn()).isPositive();
        }

        @Test
        @DisplayName("应该生成 Base64 编码的 PNG 图片")
        void shouldGenerateBase64EncodedPngImage() {
            // given
            CaptchaRequest request = CaptchaRequest.ofImage();

            // when
            CaptchaResponse response = captchaService.generate(request);

            // then
            assertThat(response.captchaImage()).isNotBlank();
            // 验证是否为有效的 Base64 编码
            assertThatCode(() -> Base64.getDecoder().decode(response.captchaImage()))
                    .doesNotThrowAnyException();
            // PNG 图片以 89 50 4E 47 开头
            byte[] imageBytes = Base64.getDecoder().decode(response.captchaImage());
            assertThat(imageBytes[0]).isEqualTo((byte) 0x89);
            assertThat(imageBytes[1]).isEqualTo((byte) 0x50); // P
            assertThat(imageBytes[2]).isEqualTo((byte) 0x4E); // N
            assertThat(imageBytes[3]).isEqualTo((byte) 0x47); // G
        }

        @Test
        @DisplayName("应该生成 4 位字母数字验证码")
        void shouldGenerate4DigitAlphanumericCaptcha() {
            // given
            CaptchaRequest request = CaptchaRequest.ofImage();
            ArgumentCaptor<String> captchaValueCaptor = ArgumentCaptor.forClass(String.class);

            // when
            captchaService.generate(request);

            // then
            verify(captchaStorage).save(anyString(), captchaValueCaptor.capture(), any(Duration.class));
            String captchaValue = captchaValueCaptor.getValue();
            assertThat(captchaValue).hasSize(4);
            assertThat(captchaValue).matches(Pattern.compile("[A-Za-z0-9]+"));
        }

        @Test
        @DisplayName("应该排除易混淆字符")
        void shouldExcludeConfusingCharacters() {
            // given
            CaptchaRequest request = CaptchaRequest.ofImage();
            ArgumentCaptor<String> captchaValueCaptor = ArgumentCaptor.forClass(String.class);

            // when - 生成多次验证码，检查是否包含易混淆字符
            for (int i = 0; i < 100; i++) {
                captchaService.generate(request);
            }

            // then - 验证所有生成的验证码都不包含易混淆字符
            // 易混淆字符：0O1lI
            verify(captchaStorage, org.mockito.Mockito.times(100))
                    .save(anyString(), captchaValueCaptor.capture(), any(Duration.class));
            for (String value : captchaValueCaptor.getAllValues()) {
                assertThat(value).doesNotContain("0", "O", "1", "l", "I");
            }
        }

        @Test
        @DisplayName("应该保存验证码到存储")
        void shouldSaveCaptchaToStorage() {
            // given
            CaptchaRequest request = CaptchaRequest.ofImage();

            // when
            CaptchaResponse response = captchaService.generate(request);

            // then
            verify(captchaStorage).save(
                    eq(response.captchaKey()),
                    anyString(),
                    any(Duration.class));
        }

        @Test
        @DisplayName("应该使用正确的过期时间")
        void shouldUseCorrectExpirationTime() {
            // given
            CaptchaRequest request = CaptchaRequest.ofImage();
            ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

            // when
            captchaService.generate(request);

            // then
            verify(captchaStorage).save(anyString(), anyString(), durationCaptor.capture());
            Duration ttl = durationCaptor.getValue();
            assertThat(ttl).isNotNull();
            assertThat(ttl.toSeconds()).isPositive();
        }
    }

    @Nested
    @DisplayName("验证验证码测试")
    class ValidateCaptchaTests {

        @Test
        @DisplayName("应该验证正确的验证码")
        void shouldValidateCorrectCaptcha() {
            // given
            String captchaKey = "test-key";
            String captchaValue = "ABCD";
            org.mockito.BDDMockito.given(captchaStorage.get(captchaKey)).willReturn(captchaValue);

            // when
            boolean result = captchaService.validate(captchaKey, captchaValue);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该不区分大小写验证")
        void shouldValidateCaseInsensitive() {
            // given
            String captchaKey = "test-key";
            String captchaValue = "ABCD";
            org.mockito.BDDMockito.given(captchaStorage.get(captchaKey)).willReturn(captchaValue);

            // when
            boolean result = captchaService.validate(captchaKey, "abcd");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("应该拒绝错误的验证码")
        void shouldRejectIncorrectCaptcha() {
            // given
            String captchaKey = "test-key";
            String captchaValue = "ABCD";
            org.mockito.BDDMockito.given(captchaStorage.get(captchaKey)).willReturn(captchaValue);

            // when
            boolean result = captchaService.validate(captchaKey, "WXYZ");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("应该拒绝过期的验证码")
        void shouldRejectExpiredCaptcha() {
            // given
            String captchaKey = "test-key";
            org.mockito.BDDMockito.given(captchaStorage.get(captchaKey)).willReturn(null);

            // when
            boolean result = captchaService.validate(captchaKey, "ABCD");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("验证成功后应该删除验证码")
        void shouldDeleteCaptchaAfterSuccessfulValidation() {
            // given
            String captchaKey = "test-key";
            String captchaValue = "ABCD";
            org.mockito.BDDMockito.given(captchaStorage.get(captchaKey)).willReturn(captchaValue);

            // when
            captchaService.validate(captchaKey, captchaValue);

            // then
            verify(captchaStorage).delete(captchaKey);
        }

        @Test
        @DisplayName("验证失败后不应该删除验证码")
        void shouldNotDeleteCaptchaAfterFailedValidation() {
            // given
            String captchaKey = "test-key";
            String captchaValue = "ABCD";
            org.mockito.BDDMockito.given(captchaStorage.get(captchaKey)).willReturn(captchaValue);

            // when
            captchaService.validate(captchaKey, "WRONG");

            // then
            verify(captchaStorage, never()).delete(anyString());
        }

        @Test
        @DisplayName("验证码不存在时不应该删除")
        void shouldNotDeleteWhenCaptchaNotExists() {
            // given
            String captchaKey = "test-key";
            org.mockito.BDDMockito.given(captchaStorage.get(captchaKey)).willReturn(null);

            // when
            captchaService.validate(captchaKey, "ABCD");

            // then
            verify(captchaStorage, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("删除验证码测试")
    class DeleteCaptchaTests {

        @Test
        @DisplayName("应该删除验证码")
        void shouldDeleteCaptcha() {
            // given
            String captchaKey = "test-key";

            // when
            captchaService.delete(captchaKey);

            // then
            verify(captchaStorage).delete(captchaKey);
        }
    }
}