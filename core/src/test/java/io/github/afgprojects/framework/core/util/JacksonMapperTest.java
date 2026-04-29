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

class JacksonMapperTest {

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

    @Test
    @DisplayName("应该注册 JavaTimeModule")
    void shouldRegisterJavaTimeModule() {
        ObjectMapper mapper = JacksonMapper.builder().build();

        assertThat(mapper.getRegisteredModuleIds()).contains("jackson-datatype-jsr310");
    }

    @Test
    @DisplayName("默认应该忽略 null 值")
    void default_shouldIgnoreNull() {
        ObjectMapper mapper = JacksonMapper.builder().build();

        assertThat(mapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion())
                .isEqualTo(JsonInclude.Include.NON_NULL);
    }

    @Test
    @DisplayName("默认应该忽略未知属性")
    void default_shouldIgnoreUnknownProperties() {
        ObjectMapper mapper = JacksonMapper.builder().build();

        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                .isFalse();
    }

    @Test
    @DisplayName("自定义日期格式应该生效")
    void customDateFormat_shouldWork() {
        ObjectMapper mapper = JacksonMapper.builder().dateFormat("yyyy/MM/dd").build();

        assertThat(mapper.getDateFormat()).isInstanceOf(SimpleDateFormat.class);
        SimpleDateFormat sdf = (SimpleDateFormat) mapper.getDateFormat();
        assertThat(sdf.toPattern()).isEqualTo("yyyy/MM/dd");
    }

    @Test
    @DisplayName("自定义忽略 null 配置应该生效")
    void customIgnoreNull_shouldWork() {
        ObjectMapper mapper = JacksonMapper.builder().ignoreNull(false).build();

        // 当 ignoreNull=false 时，不设置 NON_NULL，使用 Jackson 默认行为
        assertThat(mapper.getSerializationConfig().getDefaultPropertyInclusion().getValueInclusion())
                .isNotEqualTo(JsonInclude.Include.NON_NULL);
    }

    @Test
    @DisplayName("自定义忽略未知属性配置应该生效")
    void customIgnoreUnknownProperties_shouldWork() {
        ObjectMapper mapper =
                JacksonMapper.builder().ignoreUnknownProperties(false).build();

        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                .isTrue();
    }

    @Test
    @DisplayName("自定义命名策略应该生效")
    void customNamingStrategy_shouldWork() {
        ObjectMapper mapper = JacksonMapper.builder()
                .namingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();

        assertThat(mapper.getPropertyNamingStrategy()).isEqualTo(PropertyNamingStrategies.SNAKE_CASE);
    }

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
