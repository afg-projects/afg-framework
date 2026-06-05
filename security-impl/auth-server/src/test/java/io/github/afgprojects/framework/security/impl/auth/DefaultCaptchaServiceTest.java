package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.captcha.DefaultCaptchaService;
import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.CaptchaType;
import io.github.afgprojects.framework.security.core.storage.AfgCaptchaStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultCaptchaService 测试
 */
@DisplayName("DefaultCaptchaService 测试")
class DefaultCaptchaServiceTest {

    /**
     * 内存验证码存储，用于测试
     */
    private static class InMemoryCaptchaStorage implements AfgCaptchaStorage {

        private final Map<String, Entry> storage = new ConcurrentHashMap<>();

        private record Entry(String value, Instant expiresAt) {
            boolean isExpired() {
                return expiresAt.isBefore(Instant.now());
            }
        }

        @Override
        public void save(@NonNull String key, @NonNull String value, @NonNull Duration ttl) {
            storage.put(key, new Entry(value, Instant.now().plus(ttl)));
        }

        @Override
        @Nullable
        public String get(@NonNull String key) {
            Entry entry = storage.get(key);
            if (entry == null || entry.isExpired()) {
                storage.remove(key);
                return null;
            }
            return entry.value;
        }

        @Override
        public void delete(@NonNull String key) {
            storage.remove(key);
        }

        @Override
        public boolean exists(@NonNull String key) {
            Entry entry = storage.get(key);
            if (entry == null) {
                return false;
            }
            if (entry.isExpired()) {
                storage.remove(key);
                return false;
            }
            return true;
        }

        int size() {
            return storage.size();
        }

        void clear() {
            storage.clear();
        }
    }

    private DefaultCaptchaService captchaService;
    private InMemoryCaptchaStorage captchaStorage;

    @BeforeEach
    void setUp() {
        captchaStorage = new InMemoryCaptchaStorage();
        captchaService = new DefaultCaptchaService(captchaStorage);
    }

    @Nested
    @DisplayName("generate 方法")
    class GenerateTests {

        @Test
        @DisplayName("应生成图形验证码响应")
        void shouldGenerateImageCaptcha() {
            CaptchaRequest request = CaptchaRequest.ofImage();

            CaptchaResponse response = captchaService.generate(request);

            assertThat(response.captchaKey()).isNotNull();
            assertThat(response.captchaKey()).isNotBlank();
            assertThat(response.captchaImage()).isNotNull();
            assertThat(response.captchaType()).isEqualTo(CaptchaType.IMAGE);
            assertThat(response.expiresIn()).isEqualTo(300);
        }

        @Test
        @DisplayName("每次生成的 captchaKey 应唯一")
        void shouldGenerateUniqueCaptchaKey() {
            CaptchaRequest request = CaptchaRequest.ofImage();

            CaptchaResponse response1 = captchaService.generate(request);
            CaptchaResponse response2 = captchaService.generate(request);

            assertThat(response1.captchaKey()).isNotEqualTo(response2.captchaKey());
        }

        @Test
        @DisplayName("生成的图片应为 Base64 编码")
        void shouldGenerateBase64Image() {
            CaptchaRequest request = CaptchaRequest.ofImage();

            CaptchaResponse response = captchaService.generate(request);

            assertThat(response.captchaImage()).isNotBlank();
            // Base64 字符只包含 A-Z, a-z, 0-9, +, /, =，且可包含换行
            assertThat(response.captchaImage()).matches("^[A-Za-z0-9+/=\\n]+$");
        }

        @Test
        @DisplayName("验证码应保存到存储中")
        void shouldSaveCaptchaToStorage() {
            CaptchaRequest request = CaptchaRequest.ofImage();

            captchaService.generate(request);

            assertThat(captchaStorage.size()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("validate 方法")
    class ValidateTests {

        @Test
        @DisplayName("正确的验证码应验证通过")
        void shouldValidateCorrectCaptcha() {
            // 生成验证码
            CaptchaResponse response = captchaService.generate(CaptchaRequest.ofImage());

            // 从存储中取出验证码值
            String storedValue = captchaStorage.get(response.captchaKey());

            // 验证
            boolean valid = captchaService.validate(response.captchaKey(), storedValue);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("验证码不区分大小写")
        void shouldValidateCaseInsensitive() {
            CaptchaResponse response = captchaService.generate(CaptchaRequest.ofImage());
            String storedValue = captchaStorage.get(response.captchaKey());

            boolean valid = captchaService.validate(response.captchaKey(), storedValue.toLowerCase());

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("错误的验证码应验证失败")
        void shouldFailValidationForWrongCaptcha() {
            CaptchaResponse response = captchaService.generate(CaptchaRequest.ofImage());

            boolean valid = captchaService.validate(response.captchaKey(), "XXXX");

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("验证成功后应删除验证码")
        void shouldDeleteCaptchaAfterSuccessfulValidation() {
            CaptchaResponse response = captchaService.generate(CaptchaRequest.ofImage());
            String storedValue = captchaStorage.get(response.captchaKey());

            captchaService.validate(response.captchaKey(), storedValue);

            assertThat(captchaStorage.exists(response.captchaKey())).isFalse();
        }

        @Test
        @DisplayName("验证失败后验证码应仍存在")
        void shouldKeepCaptchaAfterFailedValidation() {
            CaptchaResponse response = captchaService.generate(CaptchaRequest.ofImage());

            captchaService.validate(response.captchaKey(), "XXXX");

            assertThat(captchaStorage.exists(response.captchaKey())).isTrue();
        }

        @Test
        @DisplayName("不存在的验证码 key 应验证失败")
        void shouldFailForNonExistentCaptchaKey() {
            boolean valid = captchaService.validate("non-existent-key", "XXXX");

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("验证码不能重复使用")
        void shouldNotReuseCaptcha() {
            CaptchaResponse response = captchaService.generate(CaptchaRequest.ofImage());
            String storedValue = captchaStorage.get(response.captchaKey());

            // 第一次验证通过
            boolean first = captchaService.validate(response.captchaKey(), storedValue);
            assertThat(first).isTrue();

            // 第二次应失败（验证码已被删除）
            boolean second = captchaService.validate(response.captchaKey(), storedValue);
            assertThat(second).isFalse();
        }
    }

    @Nested
    @DisplayName("delete 方法")
    class DeleteTests {

        @Test
        @DisplayName("应能手动删除验证码")
        void shouldDeleteCaptcha() {
            CaptchaResponse response = captchaService.generate(CaptchaRequest.ofImage());

            captchaService.delete(response.captchaKey());

            assertThat(captchaStorage.exists(response.captchaKey())).isFalse();
        }
    }
}