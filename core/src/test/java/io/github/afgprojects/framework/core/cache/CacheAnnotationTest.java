package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 缓存注解测试
 */
@DisplayName("缓存注解测试")
class CacheAnnotationTest {

    @Nested
    @DisplayName("Cached 注解测试")
    class CachedTests {

        @Test
        @DisplayName("应该有正确的注解属性")
        void shouldHaveCorrectAnnotationAttributes() {
            Target target = Cached.class.getAnnotation(Target.class);
            assertThat(target.value()).contains(ElementType.METHOD);

            Retention retention = Cached.class.getAnnotation(Retention.class);
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() throws NoSuchMethodException {
            assertThat(Cached.class.getMethod("key").getDefaultValue()).isEqualTo("");
            assertThat(Cached.class.getMethod("keyPrefix").getDefaultValue()).isEqualTo("");
            assertThat(Cached.class.getMethod("ttl").getDefaultValue()).isEqualTo(0L);
            assertThat(Cached.class.getMethod("timeUnit").getDefaultValue()).isEqualTo(TimeUnit.MILLISECONDS);
            assertThat(Cached.class.getMethod("cacheNull").getDefaultValue()).isEqualTo(true);
            assertThat(Cached.class.getMethod("condition").getDefaultValue()).isEqualTo("");
            assertThat(Cached.class.getMethod("unless").getDefaultValue()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("CacheEvict 注解测试")
    class CacheEvictTests {

        @Test
        @DisplayName("应该有正确的注解属性")
        void shouldHaveCorrectAnnotationAttributes() {
            Target target = CacheEvict.class.getAnnotation(Target.class);
            assertThat(target.value()).contains(ElementType.METHOD);

            Retention retention = CacheEvict.class.getAnnotation(Retention.class);
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() throws NoSuchMethodException {
            assertThat(CacheEvict.class.getMethod("key").getDefaultValue()).isEqualTo("");
            assertThat(CacheEvict.class.getMethod("keyPrefix").getDefaultValue()).isEqualTo("");
            assertThat(CacheEvict.class.getMethod("allEntries").getDefaultValue()).isEqualTo(false);
            assertThat(CacheEvict.class.getMethod("beforeInvocation").getDefaultValue()).isEqualTo(false);
            assertThat(CacheEvict.class.getMethod("condition").getDefaultValue()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("CachePut 注解测试")
    class CachePutTests {

        @Test
        @DisplayName("应该有正确的注解属性")
        void shouldHaveCorrectAnnotationAttributes() {
            Target target = CachePut.class.getAnnotation(Target.class);
            assertThat(target.value()).contains(ElementType.METHOD);

            Retention retention = CachePut.class.getAnnotation(Retention.class);
            assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
        }

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() throws NoSuchMethodException {
            assertThat(CachePut.class.getMethod("key").getDefaultValue()).isEqualTo("");
            assertThat(CachePut.class.getMethod("keyPrefix").getDefaultValue()).isEqualTo("");
            assertThat(CachePut.class.getMethod("ttl").getDefaultValue()).isEqualTo(0L);
            assertThat(CachePut.class.getMethod("timeUnit").getDefaultValue()).isEqualTo(TimeUnit.MILLISECONDS);
            assertThat(CachePut.class.getMethod("cacheNull").getDefaultValue()).isEqualTo(true);
            assertThat(CachePut.class.getMethod("condition").getDefaultValue()).isEqualTo("");
            assertThat(CachePut.class.getMethod("unless").getDefaultValue()).isEqualTo("");
        }
    }
}
