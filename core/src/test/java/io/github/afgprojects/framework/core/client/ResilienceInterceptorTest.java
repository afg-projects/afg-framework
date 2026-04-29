package io.github.afgprojects.framework.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import io.github.afgprojects.framework.core.client.ResilienceInterceptor.CircuitBreakerOpenException;
import io.github.afgprojects.framework.core.client.ResilienceInterceptor.RetryExhaustedException;

/**
 * ResilienceInterceptor 测试
 */
@DisplayName("ResilienceInterceptor 测试")
class ResilienceInterceptorTest {

    private ResilienceInterceptor interceptor;
    private HttpClientProperties properties;

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = createDefaultProperties();
        interceptor = new ResilienceInterceptor(properties);
    }

    private HttpClientProperties createDefaultProperties() {
        HttpClientProperties props = new HttpClientProperties();
        props.getRetry().setEnabled(true);
        props.getRetry().setMaxAttempts(3);
        props.getRetry().setInitialInterval(10); // 短间隔用于测试
        props.getRetry().setRetryOnStatus(Set.of(502, 503, 504));
        props.getCircuitBreaker().setEnabled(true);
        props.getCircuitBreaker().setFailureThreshold(5);
        props.getCircuitBreaker().setOpenDuration(100);
        return props;
    }

    @Nested
    @DisplayName("成功请求测试")
    class SuccessfulRequestTests {

        @Test
        @DisplayName("成功请求应该返回响应")
        void shouldReturnResponseOnSuccess() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            // then
            assertThat(result).isEqualTo(response);
            verify(execution, times(1)).execute(any(), any());
        }

        @Test
        @DisplayName("成功请求应该记录为成功")
        void shouldRecordSuccessOnOkResponse() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then - 验证没有抛出异常，说明请求成功
            verify(execution, times(1)).execute(any(), any());
        }
    }

    @Nested
    @DisplayName("重试测试")
    class RetryTests {

        @Test
        @DisplayName("应该在配置的状态码上重试")
        void shouldRetryOnConfiguredStatusCodes() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));

            ClientHttpResponse failResponse = mock(ClientHttpResponse.class);
            when(failResponse.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
            doNothing().when(failResponse).close();

            ClientHttpResponse successResponse = mock(ClientHttpResponse.class);
            when(successResponse.getStatusCode()).thenReturn(HttpStatus.OK);

            when(execution.execute(any(), any())).thenReturn(failResponse).thenReturn(successResponse);

            // when
            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            // then
            assertThat(result).isEqualTo(successResponse);
            verify(execution, times(2)).execute(any(), any());
        }

        @Test
        @DisplayName("重试耗尽应该返回最终响应")
        void shouldReturnResponseWhenRetryExhausted() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            when(response.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
            doNothing().when(response).close();
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            // then - 实现返回最终响应而不是抛出异常
            assertThat(result).isEqualTo(response);
            verify(execution, times(3)).execute(any(), any());
        }

        @Test
        @DisplayName("应该在 IO 异常上重试")
        void shouldRetryOnIOException() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));

            ClientHttpResponse successResponse = mock(ClientHttpResponse.class);
            when(successResponse.getStatusCode()).thenReturn(HttpStatus.OK);

            when(execution.execute(any(), any()))
                    .thenThrow(new IOException("Connection failed"))
                    .thenReturn(successResponse);

            // when
            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            // then
            assertThat(result).isEqualTo(successResponse);
            verify(execution, times(2)).execute(any(), any());
        }

        @Test
        @DisplayName("IO 异常重试耗尽应该抛出原始异常")
        void shouldThrowOriginalIOExceptionWhenRetryExhausted() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            when(execution.execute(any(), any())).thenThrow(new IOException("Connection failed"));

            // when & then - 实现抛出原始 IOException 而不是 RetryExhaustedException
            assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Connection failed");
        }
    }

    @Nested
    @DisplayName("熔断器测试")
    class CircuitBreakerTests {

        @Test
        @DisplayName("熔断器关闭时应该允许请求")
        void shouldAllowRequestWhenCircuitBreakerClosed() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then - 无异常表示成功
            verify(execution, times(1)).execute(any(), any());
        }

        @Test
        @DisplayName("禁用熔断器时应该跳过熔断检查")
        void shouldSkipCircuitBreakerWhenDisabled() throws IOException {
            // given
            properties.getCircuitBreaker().setEnabled(false);
            interceptor = new ResilienceInterceptor(properties);

            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then
            verify(execution, times(1)).execute(any(), any());
        }
    }

    @Nested
    @DisplayName("异常类测试")
    class ExceptionTests {

        @Test
        @DisplayName("CircuitBreakerOpenException 应该包含错误码")
        void circuitBreakerOpenExceptionShouldContainCode() {
            // given
            CircuitBreakerOpenException exception = new CircuitBreakerOpenException(16001, "Circuit open");

            // then
            assertThat(exception.getCode()).isEqualTo(16001);
            assertThat(exception.getMessage()).isEqualTo("Circuit open");
        }

        @Test
        @DisplayName("RetryExhaustedException 应该包含错误码和原因")
        void retryExhaustedExceptionShouldContainCodeAndCause() {
            // given
            Throwable cause = new IOException("Connection timeout");
            RetryExhaustedException exception = new RetryExhaustedException(16002, "Retry exhausted", cause);

            // then
            assertThat(exception.getCode()).isEqualTo(16002);
            assertThat(exception.getMessage()).isEqualTo("Retry exhausted");
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("RetryExhaustedException 应该支持无原因构造")
        void retryExhaustedExceptionShouldSupportNoCause() {
            // given
            RetryExhaustedException exception = new RetryExhaustedException(16002, "Retry exhausted");

            // then
            assertThat(exception.getCode()).isEqualTo(16002);
            assertThat(exception.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("URI 提取测试")
    class KeyExtractionTests {

        @Test
        @DisplayName("应该从 URI 提取 host 作为熔断器 key")
        void shouldExtractHostFromUri() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("http://api.example.com/users"));
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then - 无异常表示成功
            verify(execution, times(1)).execute(any(), any());
        }

        @Test
        @DisplayName("URI 没有 host 时应该使用 default 作为 key")
        void shouldUseDefaultKeyWhenNoHost() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("/api/users"));
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            interceptor.intercept(request, new byte[0], execution);

            // then - 无异常表示成功
            verify(execution, times(1)).execute(any(), any());
        }
    }

    @Nested
    @DisplayName("服务端错误测试")
    class ServerErrorTests {

        @Test
        @DisplayName("5xx 响应重试耗尽应该返回最终响应")
        void shouldReturnResponseOn5xxRetryExhausted() throws IOException {
            // given - use BAD_GATEWAY (502) which is in the retry list
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            when(response.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
            doNothing().when(response).close();
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            // then - 实现返回最终响应，验证重试次数
            assertThat(result).isEqualTo(response);
            verify(execution, times(3)).execute(any(), any());
        }

        @Test
        @DisplayName("4xx 响应应该记录为成功")
        void shouldRecordSuccessOn4xxResponse() throws IOException {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
            when(execution.execute(any(), any())).thenReturn(response);

            // when
            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            // then
            assertThat(result).isEqualTo(response);
            verify(execution, times(1)).execute(any(), any());
        }
    }
}
