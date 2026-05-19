package io.github.afgprojects.framework.data.core.metadata;

import io.github.afgprojects.framework.data.core.exception.MetadataLoadException;

/**
 * 反射元数据加载器
 * <p>
 * 使用运行时反射加载实体元数据，作为 APT 的降级方案。
 * 当 APT 生成的元数据类不存在时，使用此加载器。
 *
 * <p>优先级：最低（1000），仅在其他加载器都无法加载时使用。
 *
 * <p>注意：此加载器依赖 data-jdbc 模块中的 ReflectiveEntityMetadata 类。
 * 如果 data-jdbc 模块不在类路径中，此加载器将不可用。
 *
 * <pre>
 * 示例：
 * {@code
 * ReflectiveMetadataLoader loader = new ReflectiveMetadataLoader();
 * if (loader.supports(User.class)) {
 *     EntityMetadata<User> metadata = loader.load(User.class);
 * }
 * }
 * </pre>
 *
 * @see io.github.afgprojects.framework.data.jdbc.metadata.ReflectiveEntityMetadata
 */
public class ReflectiveMetadataLoader implements EntityMetadataLoader {

    private static final String REFLECTIVE_METADATA_CLASS =
        "io.github.afgprojects.framework.data.jdbc.metadata.ReflectiveEntityMetadata";

    private volatile boolean available;
    private volatile boolean checked = false;

    @Override
    @SuppressWarnings("unchecked")
    public <T> EntityMetadata<T> load(Class<T> entityClass) {
        if (!isAvailable()) {
            return null;
        }

        try {
            Class<?> reflectiveClass = Class.forName(REFLECTIVE_METADATA_CLASS);
            java.lang.reflect.Method createMethod = reflectiveClass.getMethod("create", Class.class);
            return (EntityMetadata<T>) createMethod.invoke(null, entityClass);
        } catch (ClassNotFoundException e) {
            throw new MetadataLoadException(
                    "ReflectiveEntityMetadata class not found. Ensure data-jdbc module is on the classpath.",
                    entityClass.getName(), e);
        } catch (NoSuchMethodException e) {
            throw new MetadataLoadException(
                    "ReflectiveEntityMetadata.create method not found",
                    entityClass.getName(), e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new MetadataLoadException(
                    "Failed to create reflective metadata: " + (cause != null ? cause.getMessage() : e.getMessage()),
                    entityClass.getName(), cause != null ? cause : e);
        } catch (IllegalAccessException e) {
            throw new MetadataLoadException(
                    "Cannot access ReflectiveEntityMetadata.create method",
                    entityClass.getName(), e);
        }
    }

    @Override
    public boolean supports(Class<?> entityClass) {
        return isAvailable();
    }

    @Override
    public int getPriority() {
        return 1000; // 最低优先级
    }

    @Override
    public String getName() {
        return "Reflective";
    }

    /**
     * 检查 ReflectiveEntityMetadata 是否可用
     */
    private boolean isAvailable() {
        if (!checked) {
            synchronized (this) {
                if (!checked) {
                    try {
                        Class.forName(REFLECTIVE_METADATA_CLASS);
                        available = true;
                    } catch (ClassNotFoundException e) {
                        available = false;
                    }
                    checked = true;
                }
            }
        }
        return available;
    }
}
