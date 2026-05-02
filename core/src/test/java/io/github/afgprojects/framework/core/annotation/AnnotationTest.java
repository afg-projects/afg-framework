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
 * 注解测试
 */
@DisplayName("注解测试")
class AnnotationTest {

    @Nested
    @DisplayName("DelayTask 注解测试")
    class DelayTaskTests {

        @Test
        @DisplayName("应该有正确的注解属性")
        void shouldHaveCorrectAnnotationAttributes() {
            Target target = DelayTask.class.getAnnotation(Target.class);
            assertThat(target.value()).contains(ElementType.METHOD);

            Retention retention = DelayTask.class.getAnnotation(Retention.class);
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() throws NoSuchMethodException {
            // 通过反射获取默认值
            assertThat(DelayTask.class.getMethod("description").getDefaultValue()).isEqualTo("");
            assertThat(DelayTask.class.getMethod("concurrency").getDefaultValue()).isEqualTo(1);
            assertThat(DelayTask.class.getMethod("enabled").getDefaultValue()).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("DistributedTask 注解测试")
    class DistributedTaskTests {

        @Test
        @DisplayName("应该有正确的注解属性")
        void shouldHaveCorrectAnnotationAttributes() {
            Target target = DistributedTask.class.getAnnotation(Target.class);
            assertThat(target.value()).contains(ElementType.METHOD);

            Retention retention = DistributedTask.class.getAnnotation(Retention.class);
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

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

    @Nested
    @DisplayName("ScheduledTask 注解测试")
    class ScheduledTaskTests {

        @Test
        @DisplayName("应该有正确的注解属性")
        void shouldHaveCorrectAnnotationAttributes() {
            Target target = ScheduledTask.class.getAnnotation(Target.class);
            assertThat(target.value()).contains(ElementType.METHOD);

            Retention retention = ScheduledTask.class.getAnnotation(Retention.class);
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

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