package io.github.afgprojects.framework.integration.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.integration.redis.scheduler.RedissonDelayQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RedissonDelayQueue 集成测试
 *
 * <p>基于真实 Redis 容器测试延迟队列操作
 */
@DisplayName("RedissonDelayQueue 延迟队列测试")
class RedissonDelayQueueTest extends BaseRedisTest {

    private RedissonDelayQueue<String> delayQueue;

    @BeforeEach
    void setUp() {
        delayQueue = new RedissonDelayQueue<>(getRedissonClient(), "test-delay-queue", 1);
    }

    @AfterEach
    void tearDown() {
        if (delayQueue != null) {
            try {
                delayQueue.stop();
            } catch (Exception ignored) {
                // ignore stop errors during cleanup
            }
        }
    }

    @Nested
    @DisplayName("offer 操作（未启动队列时）")
    class OfferWithoutStart {

        @Test
        @DisplayName("未启动时 offer 应抛出异常")
        void shouldThrowException_whenQueueNotStarted() {
            assertThatThrownBy(() -> delayQueue.offer("task-1", "payload", Duration.ofSeconds(1)))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("start / stop 操作")
    class StartStop {

        @Test
        @DisplayName("start 后 stop 应正常关闭")
        void shouldStartAndStopSuccessfully() {
            delayQueue.start();
            delayQueue.stop();
            // 不应抛异常
        }

        @Test
        @DisplayName("重复 start 应为 no-op")
        void shouldIgnoreDuplicateStart() {
            delayQueue.start();
            delayQueue.start(); // 第二次应为 no-op
            delayQueue.stop();
        }
    }

    @Nested
    @DisplayName("offer / cancel 操作（队列已启动）")
    class OfferAndCancel {

        @BeforeEach
        void startQueue() {
            delayQueue.start();
        }

        @Test
        @DisplayName("offer 应成功添加延迟任务")
        void shouldOfferDelayedTask() {
            String taskId = delayQueue.offer("task-offer", "payload", Duration.ofSeconds(10));

            assertThat(taskId).isEqualTo("task-offer");
            assertThat(delayQueue.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("offerAt 应成功添加定时任务")
        void shouldOfferScheduledTask() {
            Instant executeTime = Instant.now().plusSeconds(30);
            String taskId = delayQueue.offerAt("task-scheduled", "payload", executeTime);

            assertThat(taskId).isEqualTo("task-scheduled");
            assertThat(delayQueue.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("cancel 应成功取消延迟任务")
        void shouldCancelTask() {
            delayQueue.offer("task-cancel", "payload", Duration.ofSeconds(30));
            assertThat(delayQueue.size()).isEqualTo(1);

            boolean cancelled = delayQueue.cancel("task-cancel");
            assertThat(cancelled).isTrue();
            assertThat(delayQueue.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("cancel 不存在的任务应返回 false")
        void shouldReturnFalse_whenCancelNonexistentTask() {
            boolean cancelled = delayQueue.cancel("nonexistent-task");
            assertThat(cancelled).isFalse();
        }
    }

    @Nested
    @DisplayName("任务执行")
    class TaskExecution {

        @Test
        @DisplayName("立即执行的任务应被处理器处理")
        void shouldProcessImmediateTask() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> processedTaskId = new AtomicReference<>();

            delayQueue.registerProcessor((taskId, payload) -> {
                processedTaskId.set(taskId);
                latch.countDown();
                return CompletableFuture.completedFuture(null);
            });

            delayQueue.start();

            // 添加立即执行的任务（delay=0）
            delayQueue.offerAt("immediate-task", "payload", Instant.now());

            boolean processed = latch.await(5, TimeUnit.SECONDS);
            assertThat(processed).isTrue();
            assertThat(processedTaskId.get()).isEqualTo("immediate-task");
        }
    }

    @Nested
    @DisplayName("size / pendingCount 操作")
    class SizeOperations {

        @BeforeEach
        void startQueue() {
            delayQueue.start();
        }

        @Test
        @DisplayName("size 应返回待处理任务数")
        void shouldReturnPendingTaskCount() {
            assertThat(delayQueue.size()).isEqualTo(0);

            delayQueue.offer("size-task-1", "payload", Duration.ofSeconds(30));
            delayQueue.offer("size-task-2", "payload", Duration.ofSeconds(30));

            assertThat(delayQueue.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("pendingCount 应返回待处理任务数")
        void shouldReturnPendingCount() {
            delayQueue.offer("pending-task", "payload", Duration.ofSeconds(30));
            assertThat(delayQueue.pendingCount()).isGreaterThanOrEqualTo(1);
        }
    }
}
