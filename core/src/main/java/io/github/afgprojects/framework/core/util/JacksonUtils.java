package io.github.afgprojects.framework.core.util;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

/**
 * Jackson JSON 工具类
 * 提供静态默认 ObjectMapper，支持 Spring 注入自定义 ObjectMapper（仅允许设置一次）
 */
@SuppressWarnings("PMD.AvoidUsingVolatile")
public final class JacksonUtils {

    /**
     * -- GETTER --
     *  获取当前 ObjectMapper 实例
     */
    @Getter
    // NOPMD - volatile 用于线程安全是合理的
    private static volatile @NonNull ObjectMapper objectMapper =
            JacksonMapper.builder().build();
    // NOPMD - volatile 用于线程安全是合理的
    private static volatile boolean initialized;

    private JacksonUtils() {}

    /**
     * 设置 ObjectMapper 实例（供 Spring 注入 Bean 使用）
     * <p>
     * 幂等操作：如果已设置且传入相同实例，则忽略；如果传入不同实例则抛异常。
     * 这支持 Spring 上下文缓存场景（多个测试类共享同一上下文）。
     *
     * @param mapper ObjectMapper 实例
     * @throws IllegalStateException 如果已设置过不同的实例
     */
    public static void setObjectMapper(@NonNull ObjectMapper mapper) {
        if (initialized) {
            // 幂等检查：如果是同一个实例，忽略重复设置
            if (objectMapper == mapper) {
                return;
            }
            throw new IllegalStateException(
                    "ObjectMapper has already been initialized with a different instance. " +
                    "Use JacksonUtils.reset() in tests to reinitialize.");
        }
        objectMapper = mapper;
        initialized = true;
    }

    /**
     * 重置 ObjectMapper 为默认实例（仅用于测试）
     */
    public static void reset() {
        objectMapper = JacksonMapper.builder().build();
        initialized = false;
    }

    /**
     * 对象转 JSON 字符串
     */
    @NonNull public static String toJson(@Nullable Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new JsonProcessingException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * JSON 字符串转对象
     */
    public static <T> @Nullable T parse(@NonNull String json, @NonNull Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new JsonProcessingException("Failed to deserialize JSON to object", e);
        }
    }

    /**
     * JSON 字符串转 List
     */
    @NonNull public static <T> List<T> parseList(@NonNull String json, @NonNull Class<T> clazz) {
        try {
            return objectMapper.readValue(
                    json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new JsonProcessingException("Failed to deserialize JSON to List", e);
        }
    }

    /**
     * JSON 字符串转 Map
     */
    @NonNull public static Map<String, Object> parseMap(@NonNull String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new JsonProcessingException("Failed to deserialize JSON to Map", e);
        }
    }

    /**
     * 对象转 Map
     */
    @NonNull public static Map<String, Object> toMap(@Nullable Object obj) {
        try {
            return objectMapper.convertValue(obj, new TypeReference<>() {});
        } catch (IllegalArgumentException e) {
            throw new JsonProcessingException("Failed to convert object to Map", e);
        }
    }

    /**
     * Map 转对象
     */
    public static <T> @Nullable T toObject(@NonNull Map<String, Object> map, @NonNull Class<T> clazz) {
        try {
            return objectMapper.convertValue(map, clazz);
        } catch (IllegalArgumentException e) {
            throw new JsonProcessingException("Failed to convert Map to object", e);
        }
    }

    /**
     * 深拷贝（通过序列化/反序列化实现）
     */
    public static <T> @Nullable T deepCopy(@Nullable T obj, @NonNull Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        return parse(toJson(obj), clazz);
    }
}
