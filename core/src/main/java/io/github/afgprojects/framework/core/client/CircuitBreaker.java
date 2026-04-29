package io.github.afgprojects.framework.core.client;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.NonNull;

/**
 * 熔断器
 * 实现断路器模式，防止级联故障
 */
public class CircuitBreaker {

    /**
     * 熔断器状态
     */
    public enum State {
        /**
         * 关闭状态 - 正常调用
         */
        CLOSED,
        /**
         * 开启状态 - 拒绝调用
         */
        OPEN,
        /**
         * 半开状态 - 允许部分调用测试
         */
        HALF_OPEN
    }

    private final String name;
    private final int failureThreshold;
    private final Duration openDuration;
    private final int halfOpenMaxCalls;
    private final int successThreshold;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenCalls = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    /**
     * 创建熔断器
     *
     * @param name             熔断器名称
     * @param failureThreshold 失败次数阈值
     * @param openDuration     开启持续时间
     * @param halfOpenMaxCalls 半开状态最大调用次数
     * @param successThreshold 成功次数阈值
     */
    public CircuitBreaker(
            @NonNull String name,
            int failureThreshold,
            Duration openDuration,
            int halfOpenMaxCalls,
            int successThreshold) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
        this.successThreshold = successThreshold;
    }

    /**
     * 获取当前状态
     * <p>
     * 所有状态转换都在同步块中完成，确保线程安全。
     * 使用单一同步块避免竞态条件。
     * </p>
     */
    public State getState() {
        synchronized (this) {
            State currentState = state.get();

            // 检查是否应该从 OPEN 转换到 HALF_OPEN
            if (currentState == State.OPEN) {
                long elapsed = System.currentTimeMillis() - lastFailureTime.get();
                if (elapsed >= openDuration.toMillis()) {
                    state.set(State.HALF_OPEN);
                    halfOpenCalls.set(0);
                    successCount.set(0);
                    return State.HALF_OPEN;
                }
            }

            return currentState;
        }
    }

    /**
     * 检查是否允许调用
     *
     * @return 如果允许调用返回 true
     */
    public boolean allowRequest() {
        State currentState = getState();

        switch (currentState) {
            case CLOSED:
                return true;
            case OPEN:
                return false;
            case HALF_OPEN:
                return halfOpenCalls.incrementAndGet() <= halfOpenMaxCalls;
            default:
                return false;
        }
    }

    /**
     * 记录成功调用
     */
    public void recordSuccess() {
        synchronized (this) {
            State currentState = state.get();

            if (currentState == State.HALF_OPEN) {
                int successes = successCount.incrementAndGet();
                if (successes >= successThreshold) {
                    state.set(State.CLOSED);
                    failureCount.set(0);
                }
            } else if (currentState == State.CLOSED) {
                failureCount.set(0);
            }
        }
    }

    /**
     * 记录失败调用
     */
    public void recordFailure() {
        synchronized (this) {
            lastFailureTime.set(System.currentTimeMillis());

            State currentState = state.get();

            if (currentState == State.HALF_OPEN) {
                state.set(State.OPEN);
            } else if (currentState == State.CLOSED) {
                int failures = failureCount.incrementAndGet();
                if (failures >= failureThreshold) {
                    state.set(State.OPEN);
                }
            }
        }
    }

    /**
     * 获取失败次数
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * 获取名称
     */
    public String getName() {
        return name;
    }

    /**
     * 创建默认熔断器
     */
    public static CircuitBreaker createDefault(String name) {
        return new CircuitBreaker(name, 5, Duration.ofSeconds(30), 3, 3);
    }
}
