package io.github.afgprojects.framework.data.core.mapper.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.data.core.mapper.TypeHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * JSON/JSONB 类型处理器
 * <p>
 * 处理不同数据库对 JSON 列类型返回值的统一转换：
 * <ul>
 *   <li>PostgreSQL/KingBase/OpenGauss/GaussDB: 返回 org.postgresql.util.PGobject</li>
 *   <li>Oracle/DM: 返回 CLOB（已由 AbstractResultSetMapper.safeReadClob 转为 String）</li>
 *   <li>MySQL/SQL Server/H2: 返回 String</li>
 * </ul>
 * <p>
 * 此处理器不注册到 TypeHandlerRegistry.defaultRegistry()，
 * 而是在 EntityMapper/DtoMapper 检测到 JSON 类型字段时动态使用。
 * <p>
 * 转换规则：
 * <ul>
 *   <li>目标类型为 String/CharSequence → 直接返回 JSON 字符串</li>
 *   <li>目标类型为 Map → 反序列化为 Map</li>
 *   <li>目标类型为 List → 反序列化为 List</li>
 *   <li>其他 POJO → 使用 Jackson 反序列化</li>
 * </ul>
 */
@Slf4j
public class JsonTypeHandler implements TypeHandler<Object> {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

    private final ObjectMapper objectMapper;

    public JsonTypeHandler() {
        this.objectMapper = DEFAULT_OBJECT_MAPPER;
    }

    /**
     * 使用自定义 ObjectMapper 创建 JsonTypeHandler
     *
     * @param objectMapper 自定义 ObjectMapper 实例
     */
    public JsonTypeHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : DEFAULT_OBJECT_MAPPER;
    }

    @Override
    public Class<Object> getType() {
        return Object.class;
    }

    @Override
    public Object convert(Object value, Class<Object> targetType) {
        if (value == null) return null;

        String jsonString = extractJsonString(value);
        if (jsonString == null) return null;

        return parseJsonToType(jsonString, targetType);
    }

    /**
     * 从数据库返回值中提取 JSON 字符串
     */
    private String extractJsonString(Object value) {
        if (value == null) return null;

        // PGobject (PostgreSQL/KingBase/OpenGauss/GaussDB)
        if ("org.postgresql.util.PGobject".equals(value.getClass().getName())) {
            try {
                Method getValue = value.getClass().getMethod("getValue");
                return (String) getValue.invoke(value);
            } catch (Exception e) {
                log.debug("Failed to extract value from PGobject", e);
                return null;
            }
        }

        // CLOB 已由 AbstractResultSetMapper.safeReadClob 转为 String
        // String (MySQL/SQL Server/H2)
        if (value instanceof String str) {
            return str;
        }

        // 其他类型尝试 toString()
        return value.toString();
    }

    /**
     * 将 JSON 字符串解析为目标类型
     */
    private Object parseJsonToType(String json, Class<?> targetType) {
        try {
            if (CharSequence.class.isAssignableFrom(targetType)) {
                return json;
            }
            if (Map.class.isAssignableFrom(targetType)) {
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            }
            if (List.class.isAssignableFrom(targetType)) {
                return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
            }
            return objectMapper.readValue(json, targetType);
        } catch (Exception e) {
            log.debug("Failed to parse JSON to {}: {}", targetType.getSimpleName(), e.getMessage());
            return null;
        }
    }

    @Override
    public int priority() {
        // 最低优先级，仅当其他 TypeHandler 无法处理时使用
        return Integer.MAX_VALUE - 1;
    }
}
