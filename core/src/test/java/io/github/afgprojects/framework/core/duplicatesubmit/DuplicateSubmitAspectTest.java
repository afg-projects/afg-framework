package io.github.afgprojects.framework.core.duplicatesubmit;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

@DisplayName("DuplicateSubmit annotation and config")
class DuplicateSubmitConfigTest {

    @Nested
    @DisplayName("@DuplicateSubmit annotation defaults")
    class AnnotationDefaults {

        @Test
        @DisplayName("should have default key as empty string")
        void shouldHaveDefaultKeyAsEmptyString() throws NoSuchMethodException {
            Method method = TestService.class.getMethod("defaultMethod");
            DuplicateSubmit annotation = method.getAnnotation(DuplicateSubmit.class);
            assertThat(annotation.key()).isEmpty();
        }

        @Test
        @DisplayName("should have default interval as 3000ms")
        void shouldHaveDefaultIntervalAs3000ms() throws NoSuchMethodException {
            Method method = TestService.class.getMethod("defaultMethod");
            DuplicateSubmit annotation = method.getAnnotation(DuplicateSubmit.class);
            assertThat(annotation.interval()).isEqualTo(3000);
        }

        @Test
        @DisplayName("should have default prefix as duplicate-submit")
        void shouldHaveDefaultPrefixAsDuplicateSubmit() throws NoSuchMethodException {
            Method method = TestService.class.getMethod("defaultMethod");
            DuplicateSubmit annotation = method.getAnnotation(DuplicateSubmit.class);
            assertThat(annotation.prefix()).isEqualTo("duplicate-submit");
        }

        @Test
        @DisplayName("should have default message")
        void shouldHaveDefaultMessage() throws NoSuchMethodException {
            Method method = TestService.class.getMethod("defaultMethod");
            DuplicateSubmit annotation = method.getAnnotation(DuplicateSubmit.class);
            assertThat(annotation.message()).isEqualTo("请勿重复提交");
        }

        @Test
        @DisplayName("should allow custom key expression")
        void shouldAllowCustomKeyExpression() throws NoSuchMethodException {
            Method method = TestService.class.getMethod("customKeyMethod", String.class);
            DuplicateSubmit annotation = method.getAnnotation(DuplicateSubmit.class);
            assertThat(annotation.key()).isEqualTo("#orderId");
        }

        @Test
        @DisplayName("should allow custom interval")
        void shouldAllowCustomInterval() throws NoSuchMethodException {
            Method method = TestService.class.getMethod("customIntervalMethod");
            DuplicateSubmit annotation = method.getAnnotation(DuplicateSubmit.class);
            assertThat(annotation.interval()).isEqualTo(5000);
        }

        @Test
        @DisplayName("should allow custom message")
        void shouldAllowCustomMessage() throws NoSuchMethodException {
            Method method = TestService.class.getMethod("customMessageMethod");
            DuplicateSubmit annotation = method.getAnnotation(DuplicateSubmit.class);
            assertThat(annotation.message()).isEqualTo("订单正在处理中");
        }
    }

    @Nested
    @DisplayName("AfgCoreProperties.DuplicateSubmitConfig")
    class DuplicateSubmitConfig {

        @Test
        @DisplayName("should have enabled default as true")
        void shouldHaveEnabledDefaultAsTrue() {
            AfgCoreProperties.DuplicateSubmitConfig config = new AfgCoreProperties.DuplicateSubmitConfig();
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have default key prefix")
        void shouldHaveDefaultKeyPrefix() {
            AfgCoreProperties.DuplicateSubmitConfig config = new AfgCoreProperties.DuplicateSubmitConfig();
            assertThat(config.getKeyPrefix()).isEqualTo("afg:duplicate-submit");
        }

        @Test
        @DisplayName("should have default interval as 3000ms")
        void shouldHaveDefaultIntervalAs3000ms() {
            AfgCoreProperties.DuplicateSubmitConfig config = new AfgCoreProperties.DuplicateSubmitConfig();
            assertThat(config.getDefaultInterval()).isEqualTo(3000);
        }

        @Test
        @DisplayName("should have annotations enabled by default")
        void shouldHaveAnnotationsEnabledByDefault() {
            AfgCoreProperties.DuplicateSubmitConfig config = new AfgCoreProperties.DuplicateSubmitConfig();
            assertThat(config.getAnnotations().isEnabled()).isTrue();
        }
    }

    // Test service class with @DuplicateSubmit annotations
    @SuppressWarnings("unused")
    static class TestService {

        @DuplicateSubmit
        public void defaultMethod() {
        }

        @DuplicateSubmit(key = "#orderId")
        public void customKeyMethod(String orderId) {
        }

        @DuplicateSubmit(interval = 5000)
        public void customIntervalMethod() {
        }

        @DuplicateSubmit(message = "订单正在处理中")
        public void customMessageMethod() {
        }
    }
}
