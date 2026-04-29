package io.github.afgprojects.framework.core.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson ObjectMapper 构建器
 * 提供流式 API 创建自定义 ObjectMapper 实例
 *
 * @since 1.0.0
 */
public final class JacksonMapper {

    private JacksonMapper() {}

    /**
     * 创建 Builder 实例
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Jackson ObjectMapper 构建器
     */
    public static class Builder {

        private String dateFormat = "yyyy-MM-dd HH:mm:ss";
        private boolean ignoreNull = true;
        private boolean ignoreUnknownProperties = true;
        private PropertyNamingStrategy namingStrategy = PropertyNamingStrategies.LOWER_CAMEL_CASE;

        /**
         * 设置日期格式
         *
         * @param pattern 日期格式模式
         * @return this
         */
        public Builder dateFormat(String pattern) {
            this.dateFormat = pattern;
            return this;
        }

        /**
         * 设置是否忽略 null 值
         *
         * @param ignoreNull 是否忽略
         * @return this
         */
        public Builder ignoreNull(boolean ignoreNull) {
            this.ignoreNull = ignoreNull;
            return this;
        }

        /**
         * 设置是否忽略未知属性
         *
         * @param ignoreUnknownProperties 是否忽略
         * @return this
         */
        public Builder ignoreUnknownProperties(boolean ignoreUnknownProperties) {
            this.ignoreUnknownProperties = ignoreUnknownProperties;
            return this;
        }

        /**
         * 设置命名策略
         *
         * @param namingStrategy 命名策略
         * @return this
         */
        public Builder namingStrategy(PropertyNamingStrategy namingStrategy) {
            this.namingStrategy = namingStrategy;
            return this;
        }

        /**
         * 构建 ObjectMapper 实例
         *
         * @return 配置好的 ObjectMapper
         */
        @SuppressWarnings("PMD.ReplaceJavaUtilDate")
        public ObjectMapper build() {
            ObjectMapper mapper = new ObjectMapper();

            // 注册 Java 8 时间模块
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 解析器特性
            mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
            mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

            // 日期格式
            // 注意：SimpleDateFormat 非线程安全，但此处每次调用都创建新实例
            // ObjectMapper 内部会对其进行克隆，因此是安全的
            DateFormat df = new SimpleDateFormat(dateFormat, Locale.getDefault());
            mapper.setDateFormat(df);

            // 命名策略
            mapper.setPropertyNamingStrategy(namingStrategy);

            // 忽略 null 值
            if (ignoreNull) {
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            }

            // 忽略未知属性
            if (ignoreUnknownProperties) {
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            }

            return mapper;
        }
    }
}
