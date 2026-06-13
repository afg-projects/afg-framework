package io.github.afgprojects.framework.core.api.enummanagement;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 枚举注册表 SPI 接口。
 * <p>
 * 提供枚举元数据的注册和查询功能。
 * 本地默认实现由 Core 模块提供（{@link LocalEnumRegistry}），
 * 分布式注册表（如 Redis 或配置中心）由集成模块提供。
 *
 * <pre>{@code
 * @Autowired
 * private EnumRegistry enumRegistry;
 *
 * // 注册枚举
 * enumRegistry.register(UserStatus.class);
 *
 * // 查询枚举元数据
 * EnumMetadata metadata = enumRegistry.getMetadata("UserStatus");
 *
 * // 获取枚举项列表（供前端下拉框使用）
 * List<EnumItem> items = enumRegistry.getItems("UserStatus");
 * }</pre>
 *
 * @see EnumMetadata
 * @see EnumItem
 * @since 1.0.0
 */
public interface EnumRegistry {

    /**
     * 注册枚举类。
     * <p>
     * 自动读取枚举类上的 {@code @AfgEnum} 注解（如果存在），
     * 使用注解指定的 valueField/labelField/i18nPrefix。
     * 如果没有 {@code @AfgEnum} 注解，使用 name() 作为 label，ordinal() 作为 value。
     *
     * @param enumClass 枚举类
     */
    void register(@NonNull Class<? extends Enum<?>> enumClass);

    /**
     * 注册枚举类，手动指定值字段和标签字段。
     * <p>
     * 当枚举类没有 {@code @AfgEnum} 注解，或需要覆盖注解配置时使用。
     *
     * @param enumClass   枚举类
     * @param valueField  值字段名
     * @param labelField  标签字段名
     */
    void register(@NonNull Class<? extends Enum<?>> enumClass,
                  @NonNull String valueField, @NonNull String labelField);

    /**
     * 根据枚举名称获取元数据。
     *
     * @param enumName 枚举名称（简单类名）
     * @return 枚举元数据，不存在则返回 null
     */
    @Nullable
    EnumMetadata getMetadata(@NonNull String enumName);

    /**
     * 根据枚举类获取元数据。
     *
     * @param enumClass 枚举类
     * @return 枚举元数据，不存在则返回 null
     */
    @Nullable
    EnumMetadata getMetadata(@NonNull Class<? extends Enum<?>> enumClass);

    /**
     * 获取所有已注册的枚举元数据。
     *
     * @return 枚举元数据列表
     */
    @NonNull
    List<EnumMetadata> getAllMetadata();

    /**
     * 根据枚举名称获取枚举项列表。
     * <p>
     * 供前端下拉框使用。
     *
     * @param enumName 枚举名称
     * @return 枚举项列表，不存在则返回空列表
     */
    @NonNull
    List<EnumItem> getItems(@NonNull String enumName);
}
