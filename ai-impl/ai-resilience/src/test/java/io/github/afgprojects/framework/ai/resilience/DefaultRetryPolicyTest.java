package io.github.afgprojects.framework.ai.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultRetryPolicy 单元测试
 */
class DefaultRetryPolicyTest {

    private DefaultRetryPolicy retryPolicy;

    @BeforeEach
    void setUp() {
        retryPolicy = DefaultRetryPolicy.builder()
                .maxRetries(3)
                .initialIntervalMs(100)
                .multiplier(2.0)
                .maxIntervalMs(1000)
                .jitterFactor(0.0)
                .build();
    }

    @Test
    @DisplayName("可重试异常应该重试")
    void shouldRetry_retryableException() {
        SocketTimeoutException exception = new SocketTimeoutException("timeout");

        assertThat(retryPolicy.shouldRetry(exception, 0)).isTrue();
        assertThat(retryPolicy.shouldRetry(exception, 2)).isTrue();
    }

    @Test
    @DisplayName("达到最大重试次数后不再重试")
    void shouldRetry_maxRetriesReached() {
        SocketTimeoutException exception = new SocketTimeoutException("timeout");

        assertThat(retryPolicy.shouldRetry(exception, 3)).isFalse();
        assertThat(retryPolicy.shouldRetry(exception, 5)).isFalse();
    }

    @Test
    @DisplayName("不可重试异常不重试")
    void shouldRetry_nonRetryableException() {
        IllegalArgumentException exception = new IllegalArgumentException("invalid argument");

        assertThat(retryPolicy.shouldRetry(exception, 0)).isFalse();
    }

    @Test
    @DisplayName("消息包含可重试关键字时重试")
    void shouldRetry_messageContainsRetryableKeyword() {
        RuntimeException exception = new RuntimeException("rate limit exceeded");

        assertThat(retryPolicy.shouldRetry(exception, 0)).isTrue();
    }

    @Test
    @DisplayName("计算指数退避时间")
    void getWaitTime_exponentialBackoff() {
        assertThat(retryPolicy.getWaitTime(0)).isEqualTo(100);
        assertThat(retryPolicy.getWaitTime(1)).isEqualTo(200);
        assertThat(retryPolicy.getWaitTime(2)).isEqualTo(400);
        assertThat(retryPolicy.getWaitTime(3)).isEqualTo(800);
    }

    @Test
    @DisplayName("等待时间不超过最大值")
    void getWaitTime_cappedAtMaxInterval() {
        retryPolicy = DefaultRetryPolicy.builder()
                .maxRetries(10)
                .initialIntervalMs(1000)
                .multiplier(10.0)
                .maxIntervalMs(5000)
                .jitterFactor(0.0)
                .build();

        assertThat(retryPolicy.getWaitTime(5)).isEqualTo(5000);
        assertThat(retryPolicy.getWaitTime(10)).isEqualTo(5000);
    }

    @Test
    @DisplayName("执行成功操作")
    void execute_success() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        String result = retryPolicy.execute(() -> {
            counter.incrementAndGet();
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("失败后重试成功")
    void execute_retrySuccess() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);

        String result = retryPolicy.execute(() -> {
            int count = counter.incrementAndGet();
            if (count < 3) {
                throw new SocketTimeoutException("timeout");
            }
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("所有重试失败后抛出异常")
    void execute_allRetriesFailed() {
        AtomicInteger counter = new AtomicInteger(0);

        assertThatThrownBy(() -> retryPolicy.execute(() -> {
            counter.incrementAndGet();
            throw new SocketTimeoutException("timeout");
        }))
                .isInstanceOf(SocketTimeoutException.class)
                .hasMessage("timeout");

        // 初始调用 + 3 次重试
        assertThat(counter.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("添加自定义可重试异常")
    void addRetryableException() {
        retryPolicy.addRetryableException(IllegalStateException.class);

        assertThat(retryPolicy.shouldRetry(new IllegalStateException("test"), 0)).isTrue();
    }

    @Test
    @DisplayName("获取最大重试次数")
    void getMaxRetries() {
        assertThat(retryPolicy.getMaxRetries()).isEqualTo(3);
    }
}