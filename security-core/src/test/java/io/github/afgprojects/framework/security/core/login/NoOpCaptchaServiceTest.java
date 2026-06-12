package io.github.afgprojects.framework.security.core.login;

import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpCaptchaService 测试
 */
@DisplayName("NoOpCaptchaService 测试")
class NoOpCaptchaServiceTest {

    private NoOpCaptchaService captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new NoOpCaptchaService();
    }

    @Test
    @DisplayName("generate 应返回 noop key 的空响应")
    void shouldReturnNoopCaptchaResponse() {
        CaptchaRequest request = CaptchaRequest.ofImage();

        CaptchaResponse response = captchaService.generate(request);

        assertThat(response.captchaKey()).isEqualTo("noop");
    }

    @Test
    @DisplayName("validate 应返回 false")
    void shouldReturnFalseOnValidate() {
        boolean result = captchaService.validate("any-key", "any-value");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("delete 应不抛异常")
    void shouldNotThrowOnDelete() {
        captchaService.delete("any-key");
    }
}
