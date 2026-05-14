package io.github.afgprojects.framework.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.SimpleDateFormat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * JacksonMapper 构建器测试。
 * <p>
 * 测试 JacksonMapper.Builder 的各种配置选项，包括默认配置、自定义配置以及多次构建的独立性。
 *
 * @see JacksonMapper
 * @see JacksonMapper.Builder
 */
class JacksonMapperTest {

    /**
     * 测试默认配置是否正确设置。
     * <p>
     * 验证默认配置下：禁用日期时间戳格式、允许单引号、允许未加引号的字段名。
     */
    @Test
    @DisplayName("默认配置应该正确设置")
    void defaultConfig_shouldSetupCorrectly() {
        ObjectMapper mapper = JacksonMapper.builder().build();

        assertThat(mapper).isNotNull();
        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))
                .isFalse();
        assertThat(mapper.isEnabled(JsonParser.Feature.ALLOW_SINGLE_QUOTES)).isTrue();
        assertThat(mapper.isEnabled(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES))
                .isTrue();
    }

    /**
     * 测试是否正确注册 JavaTimeModule。
     * <p>
     * 验证构建的 ObjectMapper 包含 JSR-310 时间模块，支持 Java 8 日期时间类型。
     */
    @Test
    @DisplayName("应该注册 JavaTimeModule")
    void shouldRegisterJavaTimeModule() {
        ObjectMapper mapper = JacksonMapper.builder().build();

        assertThat(mapper.getRegisteredModuleIds()).contains("jackson-datatype-jsr310");
    }

    /**
     * 测试默认配置是否忽略 null 值。
     * <p>
     * 验证序列化时默认不输出值为 null 的字段。
     */
    @Test
    @DisplayName("默认应该忽略 null 值")
    void default_shouldIgnoreNull() {
        ObjectMapper mapper = JacksonMapper.builder().build();

        assertThat(mapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion())
                .isEqualTo(JsonInclude.Include.NON_NULL);
    }

    /**
     * 测试默认配置是否忽略未知属性。
     * <p>
     * 验证反序列化时遇到未知属性不会抛出异常。
     */
    @Test
    @DisplayName("默认应该忽略未知属性")
    void default_shouldIgnoreUnknownProperties() {
        ObjectMapper mapper = JacksonMapper.builder().build();

        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                .isFalse();
    }

    /**
     * 测试自定义日期格式是否生效。
     * <p>
     * 验证通过 dateFormat 方法设置的日期格式能正确应用到 ObjectMapper。
     */
    @Test
    @DisplayName("自定义日期格式应该生效")
    void customDateFormat_shouldWork() {
        ObjectMapper mapper = JacksonMapper.builder().dateFormat("yyyy/MM/dd").build();

        assertThat(mapper.getDateFormat()).isInstanceOf(SimpleDateFormat.class);
        SimpleDateFormat sdf = (SimpleDateFormat) mapper.getDateFormat();
        assertThat(sdf.toPattern()).isEqualTo("yyyy/MM/dd");
    }

    /**
     * 测试自定义忽略 null 配置是否生效。
     * <p>
     * 验证通过 ignoreNull(false) 可以禁用默认的 null 值忽略行为。
     */
    @Test
    @DisplayName("自定义忽略 null 配置应该生效")
    void customIgnoreNull_shouldWork() {
        ObjectMapper mapper = JacksonMapper.builder().ignoreNull(false).build();

        // 当 ignoreNull=false 时，不设置 NON_NULL，使用 Jackson 默认行为
        assertThat(mapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion())
                .isNotEqualTo(JsonInclude.Include.NON_NULL);
    }

    /**
     * 测试自定义忽略未知属性配置是否生效。
     * <p>
     * 验证通过 ignoreUnknownProperties(false) 可以在遇到未知属性时抛出异常。
     */
    @Test
    @DisplayName("自定义忽略未知属性配置应该生效")
    void customIgnoreUnknownProperties_shouldWork() {
        ObjectMapper mapper =
                JacksonMapper.builder().ignoreUnknownProperties(false).build();

        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                .isTrue();
    }

    /**
     * 测试自定义命名策略是否生效。
     * <p>
     * 验证通过 namingStrategy 方法设置的属性命名策略能正确应用到 ObjectMapper。
     */
    @Test
    @DisplayName("自定义命名策略应该生效")
    void customNamingStrategy_shouldWork() {
        ObjectMapper mapper = JacksonMapper.builder()
                .namingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();

        assertThat(mapper.getPropertyNamingStrategy()).isEqualTo(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * 测试多次 build 是否返回独立实例。
     * <p>
     * 验证同一个 Builder 多次调用 build() 会返回独立的 ObjectMapper 实例，
     * 且各实例的配置互不影响。
     */
    @Test
    @DisplayName("多次 build 应该返回独立实例")
    void multipleBuild_shouldReturnIndependentInstances() {
        JacksonMapper.Builder builder = JacksonMapper.builder();

        ObjectMapper m1 = builder.dateFormat("yyyy").build();
        ObjectMapper m2 = builder.dateFormat("MM-dd").build();

        assertThat(m1).isNotSameAs(m2);
        SimpleDateFormat sdf1 = (SimpleDateFormat) m1.getDateFormat();
        SimpleDateFormat sdf2 = (SimpleDateFormat) m2.getDateFormat();
        assertThat(sdf1.toPattern()).isEqualTo("yyyy");
        assertThat(sdf2.toPattern()).isEqualTo("MM-dd");
    }
}
