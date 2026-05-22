package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.config.AfgCoreProperties;

/**
 * SensitiveFieldProcessor 测试。
 * <p>
 * 测试敏感字段处理器的核心功能，包括敏感字段合并、识别和标准化。
 *
 * @see SensitiveFieldProcessor
 * @see AfgCoreProperties.AuditConfig
 */
@DisplayName("SensitiveFieldProcessor 测试")
class SensitiveFieldProcessorTest {

    private AfgCoreProperties properties;
    private SensitiveFieldProcessor processor;

    /**
     * 初始化测试用的属性和处理器实例。
     */
    @BeforeEach
    void setUp() {
        properties = new AfgCoreProperties();
        processor = new SensitiveFieldProcessor(properties);
    }

    /**
     * buildSensitiveFields 方法测试。
     * <p>
     * 验证敏感字段集合的构建功能。
     */
    @Nested
    @DisplayName("buildSensitiveFields 测试")
    class BuildSensitiveFieldsTests {

        /**
         * 测试是否能够合并注解和全局配置的敏感字段。
         */
        @Test
        @DisplayName("应该合并注解和全局配置的敏感字段")
        void shouldMergeSensitiveFields() {
            properties.getAudit().setSensitiveFields(new String[]{"globalField"});

            Audited annotation = createAuditedAnnotation(new String[]{"annotationField"});

            Set<String> result = processor.buildSensitiveFields(annotation);

            assertThat(result).contains("globalfield", "annotationfield");
        }

        /**
         * 测试是否能够标准化字段名（转换为小写）。
         */
        @Test
        @DisplayName("应该标准化字段名")
        void shouldNormalizeFieldNames() {
            Audited annotation = createAuditedAnnotation(new String[]{"User_Name", "PASS_WORD"});

            Set<String> result = processor.buildSensitiveFields(annotation);

            assertThat(result).contains("username", "password");
        }
    }

    /**
     * isSensitive 方法测试。
     * <p>
     * 验证敏感字段的识别功能。
     */
    @Nested
    @DisplayName("isSensitive 测试")
    class IsSensitiveTests {

        /**
         * 测试是否能够正确识别敏感字段。
         */
        @Test
        @DisplayName("应该识别敏感字段")
        void shouldIdentifySensitiveField() {
            Set<String> sensitiveFields = Set.of("password", "email");

            assertThat(processor.isSensitive("password", sensitiveFields)).isTrue();
            assertThat(processor.isSensitive("email", sensitiveFields)).isTrue();
        }

        /**
         * 测试识别敏感字段时是否忽略大小写和下划线。
         */
        @Test
        @DisplayName("应该忽略大小写和下划线")
        void shouldIgnoreCaseAndUnderscore() {
            Set<String> sensitiveFields = Set.of("password");

            assertThat(processor.isSensitive("PASSWORD", sensitiveFields)).isTrue();
            assertThat(processor.isSensitive("pass_word", sensitiveFields)).isTrue();
            assertThat(processor.isSensitive("PassWord", sensitiveFields)).isTrue();
        }

        /**
         * 测试非敏感字段是否返回 false。
         */
        @Test
        @DisplayName("非敏感字段应该返回 false")
        void shouldReturnFalseForNonSensitiveField() {
            Set<String> sensitiveFields = Set.of("password");

            assertThat(processor.isSensitive("username", sensitiveFields)).isFalse();
        }
    }

    /**
     * normalizeFieldName 方法测试。
     * <p>
     * 验证字段名的标准化功能。
     */
    @Nested
    @DisplayName("normalizeFieldName 测试")
    class NormalizeFieldNameTests {

        /**
         * 测试是否能够将字段名转换为小写。
         */
        @Test
        @DisplayName("应该转换为小写")
        void shouldConvertToLowerCase() {
            String result = processor.normalizeFieldName("PASSWORD");

            assertThat(result).isEqualTo("password");
        }

        /**
         * 测试是否能够移除字段名中的下划线。
         */
        @Test
        @DisplayName("应该移除下划线")
        void shouldRemoveUnderscores() {
            String result = processor.normalizeFieldName("user_name");

            assertThat(result).isEqualTo("username");
        }

        /**
         * 测试是否能够同时转换小写和移除下划线。
         */
        @Test
        @DisplayName("应该同时转换小写和移除下划线")
        void shouldConvertToLowerCaseAndRemoveUnderscores() {
            String result = processor.normalizeFieldName("USER_NAME");

            assertThat(result).isEqualTo("username");
        }
    }

    /**
     * 创建 Audited 注解的模拟实例。
     */
    private Audited createAuditedAnnotation(String[] sensitiveFields) {
        return new Audited() {
            @Override
            public String operation() { return ""; }
            @Override
            public String module() { return ""; }
            @Override
            public String[] sensitiveFields() { return sensitiveFields; }
            @Override
            public boolean recordArgs() { return true; }
            @Override
            public boolean recordResult() { return true; }
            @Override
            public String target() { return ""; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return Audited.class; }
        };
    }
}