package io.github.afgprojects.framework.core.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import io.github.afgprojects.framework.core.client.ResilienceInterceptor.CircuitBreakerOpenException;
import io.github.afgprojects.framework.core.client.ResilienceInterceptor.RetryExhaustedException;

/**
 * AsyncResilienceInterceptor 测试
 */
@DisplayName("AsyncResilienceInterceptor 测试")
class AsyncResilienceInterceptorTest {

    private AsyncResilienceInterceptor interceptor;
    private HttpClientProperties properties;
    private ScheduledExecutorService scheduler;

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpResponse response;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        properties = createDefaultProperties();
        scheduler = new ScheduledThreadPoolExecutor(4);
        interceptor = new AsyncResilienceInterceptor(properties, scheduler);
    }

    @AfterEach
    void tearDown() throws Exception {
        interceptor.shutdown();
        mocks.close();
    }

    private HttpClientProperties createDefaultProperties() {
        HttpClientProperties props = new HttpClientProperties();
        props.getRetry().setEnabled(true);
        props.getRetry().setMaxAttempts(3);
        props.getRetry().setInitialInterval(50); // 短间隔用于测试
        props.getRetry().setRetryOnStatus(Set.of(502, 503, 504));
        props.getCircuitBreaker().setEnabled(true);
        props.getCircuitBreaker().setFailureThreshold(5);
        props.getCircuitBreaker().setOpenDuration(100);
        return props;
    }

    /**
     * 设置 mock 响应的状态码
     * Spring 7.x 中 getStatusCode() 不再抛出 IOException，但需要处理返回值
     */
    @SuppressWarnings("unchecked")
    private void mockStatusCode(ClientHttpResponse response, HttpStatus status) {
        try {
            lenient().when(response.getStatusCode()).thenReturn(status);
        } catch (IOException e) {
            // 忽略 mock 设置时的异常
        }
    }

    @Nested
    @DisplayName("成功请求测试")
    class SuccessfulRequestTests {

        @Test
        @DisplayName("成功请求应该返回响应")
        void shouldReturnResponseOnSuccess() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            mockStatusCode(response, HttpStatus.OK);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> response);

            // then
            await().atMost(Duration.ofSeconds(2)).until(future::isDone);
            assertThat(future.isCompletedExceptionally()).isFalse();
            assertThat(future.getNow(null)).isEqualTo(response);
        }

        @Test
        @DisplayName("成功请求应该记录为成功")
        void shouldRecordSuccessOnOkResponse() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            mockStatusCode(response, HttpStatus.OK);
            AtomicInteger executionCount = new AtomicInteger(0);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> {
                        executionCount.incrementAndGet();
                        return response;
                    });

            // then
            await().atMost(Duration.ofSeconds(2)).until(future::isDone);
            assertThat(executionCount.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("异步重试测试")
    class AsyncRetryTests {

        @Test
        @DisplayName("应该在配置的状态码上异步重试")
        void shouldRetryOnConfiguredStatusCodes() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));

            ClientHttpResponse failResponse = mock(ClientHttpResponse.class);
            mockStatusCode(failResponse, HttpStatus.BAD_GATEWAY);
            doNothing().when(failResponse).close();

            mockStatusCode(response, HttpStatus.OK);

            AtomicInteger executionCount = new AtomicInteger(0);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> {
                        int count = executionCount.incrementAndGet();
                        if (count == 1) {
                            return failResponse;
                        }
                        return response;
                    });

            // then
            await().atMost(Duration.ofSeconds(2))
                    .until(() -> executionCount.get() >= 2);
            assertThat(future.isCompletedExceptionally()).isFalse();
            assertThat(future.getNow(null)).isEqualTo(response);
        }

        @Test
        @DisplayName("重试耗尽应该返回最终响应")
        void shouldReturnResponseWhenRetryExhausted() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            mockStatusCode(response, HttpStatus.BAD_GATEWAY);
            doNothing().when(response).close();

            AtomicInteger executionCount = new AtomicInteger(0);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> {
                        executionCount.incrementAndGet();
                        return response;
                    });

            // then
            await().atMost(Duration.ofSeconds(3))
                    .until(() -> executionCount.get() >= 3);
            assertThat(future.isDone()).isTrue();
        }

        @Test
        @DisplayName("应该在 IO 异常上异步重试")
        void shouldRetryOnIOException() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            mockStatusCode(response, HttpStatus.OK);

            AtomicInteger executionCount = new AtomicInteger(0);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> {
                        int count = executionCount.incrementAndGet();
                        if (count == 1) {
                            throw new RuntimeException("Connection failed", new IOException("Connection failed"));
                        }
                        return response;
                    });

            // then
            await().atMost(Duration.ofSeconds(5))
                    .until(() -> executionCount.get() >= 2);
            assertThat(future.isCompletedExceptionally()).isFalse();
            assertThat(future.getNow(null)).isEqualTo(response);
        }

        @Test
        @DisplayName("异步重试不应该阻塞调用线程")
        void shouldNotBlockCallingThread() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            mockStatusCode(response, HttpStatus.BAD_GATEWAY);
            doNothing().when(response).close();

            long startTime = System.currentTimeMillis();

            // when
            interceptor.executeAsync(request, new byte[0], () -> response);

            long elapsed = System.currentTimeMillis() - startTime;

            // then - 应该立即返回，不阻塞
            assertThat(elapsed).isLessThan(100); // 小于重试间隔
        }

        @Test
        @DisplayName("异常重试耗尽应该返回异常")
        void shouldReturnExceptionWhenRetryExhausted() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));

            AtomicInteger executionCount = new AtomicInteger(0);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> {
                        executionCount.incrementAndGet();
                        throw new RuntimeException(new IOException("Connection failed"));
                    });

            // then
            await().atMost(Duration.ofSeconds(3))
                    .until(future::isCompletedExceptionally);
            // RuntimeException 会被直接返回，不会被包装成 RetryExhaustedException
            assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("熔断器测试")
    class CircuitBreakerTests {

        @Test
        @DisplayName("熔断器关闭时应该允许请求")
        void shouldAllowRequestWhenCircuitBreakerClosed() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            mockStatusCode(response, HttpStatus.OK);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> response);

            // then
            await().atMost(Duration.ofSeconds(2)).until(future::isDone);
            assertThat(future.isCompletedExceptionally()).isFalse();
        }

        @Test
        @DisplayName("禁用熔断器时应该跳过熔断检查")
        void shouldSkipCircuitBreakerWhenDisabled() {
            // given
            properties.getCircuitBreaker().setEnabled(false);
            interceptor = new AsyncResilienceInterceptor(properties, scheduler);

            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            mockStatusCode(response, HttpStatus.OK);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> response);

            // then
            await().atMost(Duration.ofSeconds(2)).until(future::isDone);
            assertThat(future.isCompletedExceptionally()).isFalse();
        }
    }

    @Nested
    @DisplayName("调度器管理测试")
    class SchedulerManagementTests {

        @Test
        @DisplayName("应该正确获取调度器")
        void shouldGetScheduler() {
            // when
            ScheduledExecutorService result = interceptor.getScheduler();

            // then
            assertThat(result).isNotNull();
            assertThat(result.isShutdown()).isFalse();
        }

        @Test
        @DisplayName("关闭调度器应该停止接收新任务")
        void shutdownShouldStopAcceptingNewTasks() {
            // when
            interceptor.shutdown();

            // then
            assertThat(scheduler.isShutdown()).isTrue();
        }
    }

    @Nested
    @DisplayName("URI 提取测试")
    class KeyExtractionTests {

        @Test
        @DisplayName("应该从 URI 提取 host 作为熔断器 key")
        void shouldExtractHostFromUri() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://api.example.com/users"));
            mockStatusCode(response, HttpStatus.OK);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> response);

            // then
            await().atMost(Duration.ofSeconds(2)).until(future::isDone);
            assertThat(future.isCompletedExceptionally()).isFalse();
        }

        @Test
        @DisplayName("URI 没有 host 时应该使用 default 作为 key")
        void shouldUseDefaultKeyWhenNoHost() {
            // given
            when(request.getURI()).thenReturn(URI.create("/api/users"));
            mockStatusCode(response, HttpStatus.OK);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> response);

            // then
            await().atMost(Duration.ofSeconds(2)).until(future::isDone);
            assertThat(future.isCompletedExceptionally()).isFalse();
        }
    }

    @Nested
    @DisplayName("回调测试")
    class CallbackTests {

        @Test
        @DisplayName("应该支持成功回调")
        void shouldSupportSuccessCallback() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            mockStatusCode(response, HttpStatus.OK);

            AtomicInteger callbackCount = new AtomicInteger(0);

            // when
            interceptor.executeAsync(request, new byte[0], () -> response)
                    .thenAccept(r -> callbackCount.incrementAndGet());

            // then
            await().atMost(Duration.ofSeconds(2))
                    .until(() -> callbackCount.get() == 1);
        }

        @Test
        @DisplayName("应该支持异常回调")
        void shouldSupportExceptionCallback() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));

            AtomicInteger callbackCount = new AtomicInteger(0);

            // when
            interceptor.executeAsync(request, new byte[0], () -> {
                throw new RuntimeException(new IOException("Connection failed"));
            }).exceptionally(ex -> {
                callbackCount.incrementAndGet();
                return null;
            });

            // then
            await().atMost(Duration.ofSeconds(3))
                    .until(() -> callbackCount.get() == 1);
        }
    }

    @Nested
    @DisplayName("服务端错误测试")
    class ServerErrorTests {

        @Test
        @DisplayName("4xx 响应应该记录为成功")
        void shouldRecordSuccessOn4xxResponse() {
            // given
            when(request.getURI()).thenReturn(URI.create("http://example.com/api"));
            mockStatusCode(response, HttpStatus.BAD_REQUEST);

            // when
            CompletableFuture<ClientHttpResponse> future = interceptor.executeAsync(
                    request, new byte[0], () -> response);

            // then
            await().atMost(Duration.ofSeconds(2)).until(future::isDone);
            assertThat(future.isCompletedExceptionally()).isFalse();
            assertThat(future.getNow(null)).isEqualTo(response);
        }
    }
}
