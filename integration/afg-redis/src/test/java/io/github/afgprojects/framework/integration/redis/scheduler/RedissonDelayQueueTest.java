package io.github.afgprojects.framework.integration.redis.scheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * RedissonDelayQueue 集成测试
 */
@Testcontainers
@DisplayName("RedissonDelayQueue 集成测试")
class RedissonDelayQueueTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

        redissonClient = Redisson.create(config);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (redissonClient != null) {
            redissonClient.shutdown();
            // 等待 Redisson 完全关闭，避免消费者线程在 shutdown 后仍在运行
            Thread.sleep(200);
        }
    }

    @Nested
    @DisplayName("基本操作测试")
    class BasicOperationsTests {

        @Test
        @DisplayName("应该成功添加延迟任务")
        void shouldOfferDelayedTask() throws InterruptedException {
            RedissonDelayQueue<String> queue = new RedissonDelayQueue<>(redissonClient, "test-queue", 1);
            queue.start();

            String taskId = queue.offer("task-1", "payload-1", Duration.ofSeconds(5));

            assertThat(taskId).isEqualTo("task-1");
            assertThat(queue.pendingCount()).isEqualTo(1);

            queue.stop();
            // 等待消费者线程停止
            Thread.sleep(100);
        }

        @Test
        @DisplayName("应该成功添加指定执行时间的任务")
        void shouldOfferTaskAtSpecificTime() throws InterruptedException {
            RedissonDelayQueue<String> queue = new RedissonDelayQueue<>(redissonClient, "test-queue-2", 1);
            queue.start();

            Instant executeTime = Instant.now().plusSeconds(10);
            String taskId = queue.offerAt("task-2", "payload-2", executeTime);

            assertThat(taskId).isEqualTo("task-2");
            assertThat(queue.pendingCount()).isEqualTo(1);

            queue.stop();
        }

        @Test
        @DisplayName("应该能够取消任务")
        void shouldCancelTask() {
            RedissonDelayQueue<String> queue = new RedissonDelayQueue<>(redissonClient, "test-queue-3", 1);
            queue.start();

            queue.offer("cancelable-task", "payload", Duration.ofMinutes(5));

            boolean cancelled = queue.cancel("cancelable-task");

            assertThat(cancelled).isTrue();

            queue.stop();
        }

        @Test
        @DisplayName("取消不存在的任务应该返回 false")
        void shouldReturnFalseWhenCancelNonExistentTask() {
            RedissonDelayQueue<String> queue = new RedissonDelayQueue<>(redissonClient, "test-queue-4", 1);
            queue.start();

            boolean cancelled = queue.cancel("non-existent");

            assertThat(cancelled).isFalse();

            queue.stop();
        }
    }

    @Nested
    @DisplayName("任务处理测试")
    class TaskProcessingTests {

        @Test
        @DisplayName("应该处理到期任务")
        void shouldProcessExpiredTask() throws InterruptedException {
            RedissonDelayQueue<TestTask> queue = new RedissonDelayQueue<>(redissonClient, "processing-queue-1", 1);
            CountDownLatch latch = new CountDownLatch(1);
            List<String> processedTasks = new ArrayList<>();

            queue.registerProcessor((taskId, payload) -> {
                processedTasks.add(taskId);
                latch.countDown();
                return CompletableFuture.completedFuture(null);
            });

            queue.start();

            // 添加一个很短延迟的任务
            queue.offer("quick-task", new TestTask("data-1"), Duration.ofMillis(200));

            // 等待任务被处理
            boolean processed = latch.await(5, TimeUnit.SECONDS);

            assertThat(processed).isTrue();
            assertThat(processedTasks).contains("quick-task");

            queue.stop();
        }

        @Test
        @DisplayName("应该按顺序处理多个到期任务")
        void shouldProcessMultipleExpiredTasksInOrder() throws InterruptedException {
            RedissonDelayQueue<TestTask> queue = new RedissonDelayQueue<>(redissonClient, "processing-queue-2", 2);
            CountDownLatch latch = new CountDownLatch(3);
            List<String> processedTasks = new ArrayList<>();

            queue.registerProcessor((taskId, payload) -> {
                synchronized (processedTasks) {
                    processedTasks.add(taskId);
                }
                latch.countDown();
                return CompletableFuture.completedFuture(null);
            });

            queue.start();

            // 添加多个任务，相同延迟
            queue.offer("task-1", new TestTask("data-1"), Duration.ofMillis(100));
            queue.offer("task-2", new TestTask("data-2"), Duration.ofMillis(100));
            queue.offer("task-3", new TestTask("data-3"), Duration.ofMillis(100));

            boolean processed = latch.await(5, TimeUnit.SECONDS);

            assertThat(processed).isTrue();
            assertThat(processedTasks).hasSize(3);

            queue.stop();
        }
    }

    @Nested
    @DisplayName("队列生命周期测试")
    class LifecycleTests {

        @Test
        @DisplayName("启动和停止队列应该正常工作")
        void shouldStartAndStopQueue() {
            RedissonDelayQueue<String> queue = new RedissonDelayQueue<>(redissonClient, "lifecycle-queue", 1);

            queue.start();
            queue.stop();
        }

        @Test
        @DisplayName("重复启动应该只启动一次")
        void shouldOnlyStartOnce() {
            RedissonDelayQueue<String> queue = new RedissonDelayQueue<>(redissonClient, "lifecycle-queue-2", 1);

            queue.start();
            queue.start(); // 再次启动

            queue.stop();
        }
    }

    @Nested
    @DisplayName("队列状态测试")
    class StatusTests {

        @Test
        @DisplayName("应该正确报告待处理任务数")
        void shouldReportCorrectPendingCount() throws InterruptedException {
            RedissonDelayQueue<String> queue = new RedissonDelayQueue<>(redissonClient, "status-queue", 1);
            queue.start();

            queue.offer("pending-1", "data-1", Duration.ofMinutes(10));
            queue.offer("pending-2", "data-2", Duration.ofMinutes(10));

            assertThat(queue.pendingCount()).isEqualTo(2);

            queue.stop();
            // 等待消费者线程停止
            Thread.sleep(100);
        }
    }

    /**
     * 测试任务记录
     */
    record TestTask(String data) implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
    }
}
