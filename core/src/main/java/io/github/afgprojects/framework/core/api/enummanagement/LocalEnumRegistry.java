package io.github.afgprojects.framework.core.api.enummanagement;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * 本地内存枚举注册表。
 * <p>
 * 使用 ConcurrentHashMap 存储枚举元数据。
 * 注册时通过反射读取枚举常量：
 * <ul>
 *   <li>{@code register(Class)} — 自动推断 value/label 字段名（启发式策略）</li>
 *   <li>{@code register(Class, valueField, labelField)} — 手动指定字段名</li>
 * </ul>
 * <p>
 * 注意：{@code @AfgEnum} 注解的 RetentionPolicy 是 SOURCE，运行时不可用。
 * APT 生成的枚举元数据类通过 SPI 机制加载，不在本类管辖范围。
 * 本类提供的是运行时注册表，供 REST 端点和前端使用。
 * <p>
 * 由 {@code EnumManagementAutoConfiguration} 作为默认实现注册，
 * 分布式注册表由集成模块提供，通过 {@code @ConditionalOnMissingBean} 自动升级。
 *
 * @since 1.0.0
 */
@Slf4j
public class LocalEnumRegistry implements EnumRegistry {

    /** 常见枚举值字段名，按优先级排列 */
    private static final Set<String> VALUE_FIELD_NAMES = Set.of(
            "code", "value", "id", "key", "status", "type", "level", "ordinal"
    );

    /** 常见枚举标签字段名，按优先级排列 */
    private static final Set<String> LABEL_FIELD_NAMES = Set.of(
            "label", "description", "name", "text", "message", "displayName", "title"
    );

    private final ConcurrentMap<String, EnumMetadata> registry = new ConcurrentHashMap<>();

    @Override
    public void register(@NonNull Class<? extends Enum<?>> enumClass) {
        // @AfgEnum 是 SOURCE 保留期，运行时不可用
        // 使用启发式策略推断字段名
        String valueField = inferValueField(enumClass);
        String labelField = inferLabelField(enumClass);
        register(enumClass, valueField, labelField);
    }

    @Override
    public void register(@NonNull Class<? extends Enum<?>> enumClass,
                         @NonNull String valueField, @NonNull String labelField) {
        String enumName = enumClass.getSimpleName();
        Enum<?>[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            log.debug("枚举 {} 没有常量，跳过注册", enumName);
            return;
        }

        List<EnumItem> items = new ArrayList<>();
        for (Enum<?> constant : constants) {
            Object value = getFieldValue(constant, valueField);
            String label = getLabelFieldValue(constant, labelField);

            items.add(EnumItem.builder()
                    .name(constant.name())
                    .value(value)
                    .label(label)
                    .ordinal(constant.ordinal())
                    .build());
        }

        EnumMetadata metadata = EnumMetadata.builder()
                .name(enumName)
                .valueField(valueField)
                .labelField(labelField)
                .i18nPrefix("")
                .items(Collections.unmodifiableList(items))
                .build();

        registry.put(enumName, metadata);
        log.debug("已注册枚举: {} (valueField={}, labelField={})", enumName, valueField, labelField);
    }

    @Override
    @Nullable
    public EnumMetadata getMetadata(@NonNull String enumName) {
        return registry.get(enumName);
    }

    @Override
    @Nullable
    public EnumMetadata getMetadata(@NonNull Class<? extends Enum<?>> enumClass) {
        return registry.get(enumClass.getSimpleName());
    }

    @Override
    @NonNull
    public List<EnumMetadata> getAllMetadata() {
        return Collections.unmodifiableList(new ArrayList<>(registry.values()));
    }

    @Override
    @NonNull
    public List<EnumItem> getItems(@NonNull String enumName) {
        EnumMetadata metadata = registry.get(enumName);
        if (metadata == null || metadata.getItems() == null) {
            return Collections.emptyList();
        }
        return metadata.getItems();
    }

    /**
     * 启发式推断值字段名。
     * <p>
     * 检查枚举类的字段，优先匹配常见值字段名（code、value、id 等）。
     * 如果没有匹配，使用 ordinal 作为值字段。
     */
    String inferValueField(Class<? extends Enum<?>> enumClass) {
        for (Field field : enumClass.getDeclaredFields()) {
            if (VALUE_FIELD_NAMES.contains(field.getName())) {
                return field.getName();
            }
        }
        return "ordinal";
    }

    /**
     * 启发式推断标签字段名。
     * <p>
     * 检查枚举类的字段，优先匹配常见标签字段名（label、description、name 等）。
     * 如果没有匹配，使用 name() 作为标签字段。
     */
    String inferLabelField(Class<? extends Enum<?>> enumClass) {
        for (Field field : enumClass.getDeclaredFields()) {
            if (LABEL_FIELD_NAMES.contains(field.getName())) {
                return field.getName();
            }
        }
        return "name";
    }

    /**
     * 通过反射获取枚举字段值。
     */
    private Object getFieldValue(Enum<?> constant, String fieldName) {
        if ("ordinal".equals(fieldName)) {
            return constant.ordinal();
        }
        if ("name".equals(fieldName)) {
            return constant.name();
        }

        try {
            // 尝试 getter 方法
            String getterName = "get" + capitalize(fieldName);
            Method getter = constant.getClass().getMethod(getterName);
            return getter.invoke(constant);
        } catch (NoSuchMethodException e) {
            // 尝试 is 前缀（布尔类型）
            try {
                String getterName = "is" + capitalize(fieldName);
                Method getter = constant.getClass().getMethod(getterName);
                return getter.invoke(constant);
            } catch (Exception ex) {
                log.debug("枚举 {} 无法获取字段 {} 的值: {}", constant.getClass().getSimpleName(), fieldName, ex.getMessage());
                return constant.ordinal();
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.debug("枚举 {} 无法获取字段 {} 的值: {}", constant.getClass().getSimpleName(), fieldName, e.getMessage());
            return constant.ordinal();
        }
    }

    /**
     * 获取标签字段值，确保返回字符串。
     */
    private String getLabelFieldValue(Enum<?> constant, String labelField) {
        Object value = getFieldValue(constant, labelField);
        return value != null ? String.valueOf(value) : constant.name();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
