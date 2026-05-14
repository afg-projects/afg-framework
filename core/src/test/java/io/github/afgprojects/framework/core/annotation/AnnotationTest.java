package io.github.afgprojects.framework.core.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 注解功能测试
 *
 * <p>测试框架核心注解的元注解配置和默认值：
 * <ul>
 *   <li>{@link DelayTask} - 延迟任务注解</li>
 *   <li>{@link DistributedTask} - 分布式任务注解</li>
 *   <li>{@link ScheduledTask} - 定时任务注解</li>
 * </ul>
 *
 * @see DelayTask
 * @see DistributedTask
 * @see ScheduledTask
 */
@DisplayName("注解测试")
class AnnotationTest {

    /**
     * {@link DelayTask} 注解测试
     * <p>验证延迟任务注解的元注解配置和属性默认值
     */
    @Nested
    @DisplayName("DelayTask 注解测试")
    class DelayTaskTests {

        /**
         * 验证注解的目标元素类型和保留策略
         * <ul>
         *   <li>目标：METHOD（方法）</li>
         *   <li>保留：RUNTIME（运行时保留，支持反射读取）</li>
         * </ul>
         */
        @Test
        @DisplayName("应该有正确的注解属性")
        void shouldHaveCorrectAnnotationAttributes() {
            Target target = DelayTask.class.getAnnotation(Target.class);
            assertThat(target.value()).contains(ElementType.METHOD);

            Retention retention = DelayTask.class.getAnnotation(Retention.class);
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

        /**
         * 验证注解属性的默认值
         * <ul>
         *   <li>description: 空字符串</li>
         *   <li>concurrency: 1（并发数默认为 1）</li>
         *   <li>enabled: true（默认启用）</li>
         * </ul>
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() throws NoSuchMethodException {
            // 通过反射获取默认值
            assertThat(DelayTask.class.getMethod("description").getDefaultValue()).isEqualTo("");
            assertThat(DelayTask.class.getMethod("concurrency").getDefaultValue()).isEqualTo(1);
            assertThat(DelayTask.class.getMethod("enabled").getDefaultValue()).isEqualTo(true);
        }
    }

    /**
     * {@link DistributedTask} 注解测试
     * <p>验证分布式任务注解的元注解配置和属性默认值
     */
    @Nested
    @DisplayName("DistributedTask 注解测试")
    class DistributedTaskTests {

        /**
         * 验证注解的目标元素类型和保留策略
         */
        @Test
        @DisplayName("应该有正确的注解属性")
        void shouldHaveCorrectAnnotationAttributes() {
            Target target = DistributedTask.class.getAnnotation(Target.class);
            assertThat(target.value()).contains(ElementType.METHOD);

            Retention retention = DistributedTask.class.getAnnotation(Retention.class);
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

        /**
         * 验证注解属性的默认值
         * <ul>
         *   <li>description: 空字符串</li>
         *   <li>lockWaitTime: 0L（锁等待时间，0 表示不等待）</li>
         *   <li>lockLeaseTime: -1L（锁租约时间，-1 表示使用默认值）</li>
         *   <li>shardCount: 1（分片数默认为 1）</li>
         *   <li>enabled: true（默认启用）</li>
         * </ul>
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() throws NoSuchMethodException {
            assertThat(DistributedTask.class.getMethod("description").getDefaultValue()).isEqualTo("");
            assertThat(DistributedTask.class.getMethod("lockWaitTime").getDefaultValue()).isEqualTo(0L);
            assertThat(DistributedTask.class.getMethod("lockLeaseTime").getDefaultValue()).isEqualTo(-1L);
            assertThat(DistributedTask.class.getMethod("shardCount").getDefaultValue()).isEqualTo(1);
            assertThat(DistributedTask.class.getMethod("enabled").getDefaultValue()).isEqualTo(true);
        }
    }

    /**
     * {@link ScheduledTask} 注解测试
     * <p>验证定时任务注解的元注解配置和属性默认值
     */
    @Nested
    @DisplayName("ScheduledTask 注解测试")
    class ScheduledTaskTests {

        /**
         * 验证注解的目标元素类型和保留策略
         */
        @Test
        @DisplayName("应该有正确的注解属性")
        void shouldHaveCorrectAnnotationAttributes() {
            Target target = ScheduledTask.class.getAnnotation(Target.class);
            assertThat(target.value()).contains(ElementType.METHOD);

            Retention retention = ScheduledTask.class.getAnnotation(Retention.class);
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

        /**
         * 验证注解属性的默认值
         * <ul>
         *   <li>cron: 空字符串（不使用 cron 表达式）</li>
         *   <li>fixedRate: -1L（不使用固定频率）</li>
         *   <li>fixedDelay: -1L（不使用固定延迟）</li>
         *   <li>initialDelay: 0L（无初始延迟）</li>
         *   <li>description: 空字符串</li>
         *   <li>enabled: true（默认启用）</li>
         *   <li>timeout: -1L（无超时限制）</li>
         *   <li>errorHandling: LOG_AND_CONTINUE（出错时记录日志并继续）</li>
         * </ul>
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() throws NoSuchMethodException {
            assertThat(ScheduledTask.class.getMethod("cron").getDefaultValue()).isEqualTo("");
            assertThat(ScheduledTask.class.getMethod("fixedRate").getDefaultValue()).isEqualTo(-1L);
            assertThat(ScheduledTask.class.getMethod("fixedDelay").getDefaultValue()).isEqualTo(-1L);
            assertThat(ScheduledTask.class.getMethod("initialDelay").getDefaultValue()).isEqualTo(0L);
            assertThat(ScheduledTask.class.getMethod("description").getDefaultValue()).isEqualTo("");
            assertThat(ScheduledTask.class.getMethod("enabled").getDefaultValue()).isEqualTo(true);
            assertThat(ScheduledTask.class.getMethod("timeout").getDefaultValue()).isEqualTo(-1L);
            assertThat(ScheduledTask.class.getMethod("errorHandling").getDefaultValue())
                    .isEqualTo(ScheduledTask.ErrorHandling.LOG_AND_CONTINUE);
        }

        /**
         * 验证 {@link ScheduledTask.ErrorHandling} 枚举包含所有错误处理策略
         * <ul>
         *   <li>LOG_AND_CONTINUE - 记录日志并继续执行</li>
         *   <li>STOP_ON_ERROR - 出错时停止</li>
         *   <li>RETRY - 重试</li>
         * </ul>
         */
        @Test
        @DisplayName("ErrorHandling 枚举应该包含所有值")
        void errorHandlingShouldContainAllValues() {
            ScheduledTask.ErrorHandling[] values = ScheduledTask.ErrorHandling.values();
            assertThat(values).containsExactly(
                    ScheduledTask.ErrorHandling.LOG_AND_CONTINUE,
                    ScheduledTask.ErrorHandling.STOP_ON_ERROR,
                    ScheduledTask.ErrorHandling.RETRY
            );
        }
    }
}