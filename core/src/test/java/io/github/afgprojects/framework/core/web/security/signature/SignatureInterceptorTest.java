package io.github.afgprojects.framework.core.web.security.signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

/**
 * SignatureInterceptor 测试
 */
@DisplayName("SignatureInterceptor 测试")
class SignatureInterceptorTest {

    private SignatureInterceptor interceptor;
    private SignatureProperties properties;
    private SignatureGenerator generator;
    private NonceCache nonceCache;

    private static final String SECRET = "test-secret-key-1234567890";
    private static final String KEY_ID = "default";

    @BeforeEach
    void setUp() {
        properties = new SignatureProperties();
        SignatureProperties.KeyConfig keyConfig = new SignatureProperties.KeyConfig();
        keyConfig.setSecret(SECRET);
        keyConfig.setEnabled(true);
        properties.getKeys().put(KEY_ID, keyConfig);

        generator = new SignatureGenerator();
        nonceCache = new NonceCache(1000);
        interceptor = new SignatureInterceptor(properties, generator, nonceCache);
    }

    @Nested
    @DisplayName("签名验证测试")
    class VerifySignatureTests {

        @Test
        @DisplayName("有效签名应该通过验证")
        void shouldPassWithValidSignature() throws Exception {
            // given
            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = "test-nonce-123";
            String body = "{\"data\":\"test\"}";
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, timestamp, nonce, body);

            HttpServletRequest request = createMockRequest(signature, timestamp, nonce, KEY_ID, body);
            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");

            // when
            boolean result = interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("缺少签名头应该抛出异常")
        void shouldThrowOnMissingSignature() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(SignatureInterceptor.HEADER_SIGNATURE)).thenReturn(null);
            when(request.getHeader(SignatureInterceptor.HEADER_TIMESTAMP)).thenReturn(String.valueOf(System.currentTimeMillis()));
            when(request.getHeader(SignatureInterceptor.HEADER_NONCE)).thenReturn("nonce");

            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");

            // when & then
            assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod))
                    .isInstanceOf(SignatureException.class)
                    .extracting("errorType")
                    .isEqualTo(SignatureException.SignatureErrorType.MISSING_SIGNATURE);
        }

        @Test
        @DisplayName("缺少时间戳应该抛出异常")
        void shouldThrowOnMissingTimestamp() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(SignatureInterceptor.HEADER_SIGNATURE)).thenReturn("signature");
            when(request.getHeader(SignatureInterceptor.HEADER_TIMESTAMP)).thenReturn(null);
            when(request.getHeader(SignatureInterceptor.HEADER_NONCE)).thenReturn("nonce");

            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");

            // when & then
            assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod))
                    .isInstanceOf(SignatureException.class)
                    .extracting("errorType")
                    .isEqualTo(SignatureException.SignatureErrorType.MISSING_TIMESTAMP);
        }

        @Test
        @DisplayName("缺少 nonce 应该抛出异常")
        void shouldThrowOnMissingNonce() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(SignatureInterceptor.HEADER_SIGNATURE)).thenReturn("signature");
            when(request.getHeader(SignatureInterceptor.HEADER_TIMESTAMP)).thenReturn(String.valueOf(System.currentTimeMillis()));
            when(request.getHeader(SignatureInterceptor.HEADER_NONCE)).thenReturn(null);

            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");

            // when & then
            assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod))
                    .isInstanceOf(SignatureException.class)
                    .extracting("errorType")
                    .isEqualTo(SignatureException.SignatureErrorType.MISSING_NONCE);
        }

        @Test
        @DisplayName("无效时间戳格式应该抛出异常")
        void shouldThrowOnInvalidTimestampFormat() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader(SignatureInterceptor.HEADER_SIGNATURE)).thenReturn("signature");
            when(request.getHeader(SignatureInterceptor.HEADER_TIMESTAMP)).thenReturn("invalid-timestamp");
            when(request.getHeader(SignatureInterceptor.HEADER_NONCE)).thenReturn("nonce");

            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");

            // when & then
            assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod))
                    .isInstanceOf(SignatureException.class)
                    .extracting("errorType")
                    .isEqualTo(SignatureException.SignatureErrorType.INVALID_TIMESTAMP);
        }

        @Test
        @DisplayName("过期时间戳应该抛出异常")
        void shouldThrowOnExpiredTimestamp() throws Exception {
            // given
            // 时间戳为 10 分钟前（超过默认的 300 秒容忍度）
            String expiredTimestamp = String.valueOf(System.currentTimeMillis() - 600000);
            HttpServletRequest request = createMockRequest("signature", expiredTimestamp, "nonce", KEY_ID, null);

            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");

            // when & then
            assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod))
                    .isInstanceOf(SignatureException.class)
                    .extracting("errorType")
                    .isEqualTo(SignatureException.SignatureErrorType.TIMESTAMP_EXPIRED);
        }

        @Test
        @DisplayName("重复 nonce 应该抛出异常")
        void shouldThrowOnNonceReuse() throws Exception {
            // given
            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = "duplicate-nonce";
            String signature = generator.generate(SignatureAlgorithm.HMAC_SHA256, SECRET, timestamp, nonce, null);

            // 第一次请求
            HttpServletRequest request1 = createMockRequest(signature, timestamp, nonce, KEY_ID, null);
            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");
            interceptor.preHandle(request1, mock(HttpServletResponse.class), handlerMethod);

            // 第二次请求（重放）
            HttpServletRequest request2 = createMockRequest(signature, timestamp, nonce, KEY_ID, null);

            // when & then
            assertThatThrownBy(() -> interceptor.preHandle(request2, mock(jakarta.servlet.http.HttpServletResponse.class), handlerMethod))
                    .isInstanceOf(SignatureException.class)
                    .extracting("errorType")
                    .isEqualTo(SignatureException.SignatureErrorType.NONCE_REUSED);
        }

        @Test
        @DisplayName("无效签名应该抛出异常")
        void shouldThrowOnInvalidSignature() throws Exception {
            // given
            HttpServletRequest request = createMockRequest("invalid-signature", String.valueOf(System.currentTimeMillis()), "nonce", KEY_ID, null);

            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");

            // when & then
            assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod))
                    .isInstanceOf(SignatureException.class)
                    .extracting("errorType")
                    .isEqualTo(SignatureException.SignatureErrorType.INVALID_SIGNATURE);
        }

        @Test
        @DisplayName("不存在的密钥应该抛出异常")
        void shouldThrowOnKeyNotFound() throws Exception {
            // given
            HttpServletRequest request = createMockRequest("signature", String.valueOf(System.currentTimeMillis()), "nonce", "nonexistent-key", null);

            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");

            // when & then
            assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod))
                    .isInstanceOf(SignatureException.class)
                    .extracting("errorType")
                    .isEqualTo(SignatureException.SignatureErrorType.KEY_NOT_FOUND);
        }

        @Test
        @DisplayName("已禁用的密钥应该抛出异常")
        void shouldThrowOnKeyDisabled() throws Exception {
            // given
            String disabledKeyId = "disabled";
            SignatureProperties.KeyConfig disabledConfig = new SignatureProperties.KeyConfig();
            disabledConfig.setSecret(SECRET);
            disabledConfig.setEnabled(false);
            properties.getKeys().put(disabledKeyId, disabledConfig);

            HttpServletRequest request = createMockRequest("signature", String.valueOf(System.currentTimeMillis()), "nonce", disabledKeyId, null);

            HandlerMethod handlerMethod = createHandlerMethod("annotatedMethod");

            // when & then
            assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod))
                    .isInstanceOf(SignatureException.class)
                    .extracting("errorType")
                    .isEqualTo(SignatureException.SignatureErrorType.KEY_DISABLED);
        }
    }

    @Nested
    @DisplayName("非签名接口测试")
    class NonSignatureTests {

        @Test
        @DisplayName("没有注解的方法应该跳过验证")
        void shouldSkipForNonAnnotatedMethod() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            HandlerMethod handlerMethod = createHandlerMethod("nonAnnotatedMethod");

            // when
            boolean result = interceptor.preHandle(request, mock(HttpServletResponse.class), handlerMethod);

            // then
            assertThat(result).isTrue();
        }
    }

    // ==================== 辅助方法 ====================

    private HttpServletRequest createMockRequest(String signature, String timestamp, String nonce, String keyId, String body) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(SignatureInterceptor.HEADER_SIGNATURE)).thenReturn(signature);
        when(request.getHeader(SignatureInterceptor.HEADER_TIMESTAMP)).thenReturn(timestamp);
        when(request.getHeader(SignatureInterceptor.HEADER_NONCE)).thenReturn(nonce);
        when(request.getHeader(SignatureInterceptor.HEADER_KEY_ID)).thenReturn(keyId);

        jakarta.servlet.ServletInputStream inputStream = mock(jakarta.servlet.ServletInputStream.class);
        if (body != null && !body.isEmpty()) {
            byte[] bodyBytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            when(inputStream.available()).thenReturn(bodyBytes.length);
            when(inputStream.readAllBytes()).thenReturn(bodyBytes);
        } else {
            when(inputStream.available()).thenReturn(0);
        }
        when(request.getInputStream()).thenReturn(inputStream);

        return request;
    }

    private HandlerMethod createHandlerMethod(String methodName) throws NoSuchMethodException {
        TestController controller = new TestController();
        Method method = TestController.class.getMethod(methodName);
        return new HandlerMethod(controller, method);
    }

    // 测试 Controller
    public static class TestController {
        @SignatureRequired
        public void annotatedMethod() {}

        public void nonAnnotatedMethod() {}
    }
}
